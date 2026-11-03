const form = document.getElementById('shorten-form');
const message = document.getElementById('form-message');
const submitBtn = document.getElementById('submit-btn');
const resultSection = document.getElementById('result-section');
const resultLink = document.getElementById('result-link');
const resultDestination = document.getElementById('result-destination');
const openBtn = document.getElementById('open-btn');
const copyBtn = document.getElementById('copy-btn');
const recentBody = document.getElementById('recent-body');
const emptyState = document.getElementById('empty-state');
const tableWrap = document.getElementById('table-wrap');
const detailsSection = document.getElementById('details-section');
const detailsTitle = document.getElementById('details-title');
const deactivateBtn = document.getElementById('deactivate-btn');
const bars = document.getElementById('bars');
let selectedCode = null;

const storageKey = 'shortpath.recentLinks.v1';
const loadRecent = () => JSON.parse(localStorage.getItem(storageKey) || '[]');
const saveRecent = (items) => localStorage.setItem(storageKey, JSON.stringify(items.slice(0, 12)));

function showMessage(text, kind='error') {
  message.textContent = text;
  message.className = `message ${kind}`;
}
function clearMessage(){ message.className='message hidden'; message.textContent=''; }
function fmtDate(value){ if(!value) return '—'; return new Intl.DateTimeFormat(undefined,{dateStyle:'medium',timeStyle:'short'}).format(new Date(value)); }
function escapeHtml(value=''){ return String(value).replace(/[&<>'"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#039;','"':'&quot;'}[c])); }
function titleCaseStatus(status){ return status ? status.charAt(0) + status.slice(1).toLowerCase() : 'Inactive'; }
function lifecycleStatus(item) {
  if (item.status === 'DEACTIVATED') return 'DEACTIVATED';
  if (item.expiresAt && new Date(item.expiresAt).getTime() <= Date.now()) return 'EXPIRED';
  if (item.status) return item.status;
  return item.active ? 'ACTIVE' : 'DEACTIVATED';
}
function statusClass(status){ return status === 'ACTIVE' ? 'pill-active' : status === 'EXPIRED' ? 'pill-expired' : 'pill-off'; }

async function api(path, options={}) {
  const response = await fetch(path, options);
  if (!response.ok) {
    let detail = `${response.status} ${response.statusText}`;
    try { const body = await response.json(); detail = body.message || body.detail || body.error || detail; } catch (_) {}
    throw new Error(detail);
  }
  if (response.status === 204) return null;
  return response.json();
}

form.addEventListener('submit', async (event) => {
  event.preventDefault(); clearMessage(); submitBtn.disabled=true; submitBtn.textContent='Creating…';
  try {
    const expiresValue = document.getElementById('expires').value;
    const body = {
      url: document.getElementById('url').value.trim(),
      customAlias: document.getElementById('alias').value.trim() || null,
      expiresAt: expiresValue ? new Date(expiresValue).toISOString() : null
    };
    const data = await api('/api/v1/urls', {
      method:'POST', headers:{'Content-Type':'application/json','Idempotency-Key': crypto.randomUUID()}, body: JSON.stringify(body)
    });
    resultLink.textContent=data.shortUrl; resultLink.href=data.shortUrl; openBtn.href=data.shortUrl; resultDestination.textContent=data.destinationUrl;
    resultSection.classList.remove('hidden'); resultSection.scrollIntoView({behavior:'smooth',block:'center'});
    const recent = loadRecent().filter(x => x.code !== data.code);
    recent.unshift(data); saveRecent(recent); renderRecent();
    form.reset();
  } catch (e) { showMessage(e.message || 'Unable to create short URL.'); }
  finally { submitBtn.disabled=false; submitBtn.innerHTML='Shorten URL <span>→</span>'; }
});

copyBtn.addEventListener('click', async () => {
  await navigator.clipboard.writeText(resultLink.textContent); copyBtn.textContent='Copied'; setTimeout(()=>copyBtn.textContent='Copy',1200);
});

document.getElementById('clear-history').addEventListener('click', () => { saveRecent([]); renderRecent(); detailsSection.classList.add('hidden'); });

function renderRecent(items = loadRecent()) {
  emptyState.classList.toggle('hidden', items.length>0); tableWrap.classList.toggle('hidden', items.length===0);
  recentBody.innerHTML = items.map(item => {
    const status = lifecycleStatus(item);
    return `
    <tr>
      <td><a href="${escapeHtml(item.shortUrl)}" target="_blank" rel="noreferrer">${escapeHtml(item.code)}</a></td>
      <td class="truncate" title="${escapeHtml(item.destinationUrl)}">${escapeHtml(item.destinationUrl)}</td>
      <td><span class="pill ${statusClass(status)}">${titleCaseStatus(status)}</span></td>
      <td>${fmtDate(item.createdAt)}</td>
      <td><button class="row-action" data-code="${escapeHtml(item.code)}">View</button></td>
    </tr>`;
  }).join('');
  document.querySelectorAll('.row-action').forEach(btn => btn.addEventListener('click', () => loadDetails(btn.dataset.code)));
}

function updateRecentMetadata(meta) {
  const items = loadRecent().map(item => item.code === meta.code ? {...item, ...meta} : item);
  saveRecent(items);
  renderRecent(items);
}

async function refreshRecentStatuses() {
  const items = loadRecent();
  if (!items.length) return;
  const refreshed = await Promise.all(items.map(async item => {
    try {
      const meta = await api(`/api/v1/urls/${encodeURIComponent(item.code)}`);
      return {...item, ...meta};
    } catch (_) {
      return item;
    }
  }));
  saveRecent(refreshed);
  renderRecent(refreshed);
}

async function loadDetails(code) {
  try {
    const [meta, analytics, eventDetails] = await Promise.all([
      api(`/api/v1/urls/${encodeURIComponent(code)}`),
      api(`/api/v1/urls/${encodeURIComponent(code)}/analytics`),
      api(`/api/v1/urls/${encodeURIComponent(code)}/analytics/events?limit=25`)
    ]);
    updateRecentMetadata(meta);
    selectedCode=code; detailsTitle.textContent=`${code} analytics`;
    document.getElementById('metric-clicks').textContent=analytics.totalClicks;
    document.getElementById('metric-last-click').textContent=fmtDate(analytics.lastClickedAt);
    const status = lifecycleStatus(meta);
    document.getElementById('metric-status').textContent=titleCaseStatus(status);
    deactivateBtn.disabled=status !== 'ACTIVE';
    renderBars(analytics.lastSevenDays || []);
    renderBreakdown('referrer-breakdown', analytics.topReferrers || []);
    renderBreakdown('browser-breakdown', analytics.browserFamilies || []);
    renderEvents(eventDetails.events || []);
    detailsSection.classList.remove('hidden'); detailsSection.scrollIntoView({behavior:'smooth',block:'start'});
  } catch(e){ alert(e.message); }
}

function renderBars(days) {
  const max=Math.max(1,...days.map(x=>x.clicks));
  bars.innerHTML=days.length ? days.map(x=>`<div class="bar-item"><div class="bar-track"><div class="bar-fill" style="height:${Math.max(6,(x.clicks/max)*100)}%"></div></div><strong>${x.clicks}</strong><span>${new Date(x.day+'T00:00:00').toLocaleDateString(undefined,{weekday:'short'})}</span></div>`).join('') : '<div class="empty-analytics">No click data yet.</div>';
}

function renderBreakdown(id, items) {
  const el = document.getElementById(id);
  if (!items.length) { el.innerHTML='<div class="empty-analytics">No data yet.</div>'; return; }
  const max=Math.max(1,...items.map(x=>x.clicks));
  el.innerHTML=items.map(item=>`<div class="breakdown-row"><span title="${escapeHtml(item.value)}">${escapeHtml(item.value)}</span><div><i style="width:${Math.max(8,(item.clicks/max)*100)}%"></i></div><strong>${item.clicks}</strong></div>`).join('');
}

function renderEvents(events) {
  const body=document.getElementById('event-body');
  if (!events.length) { body.innerHTML='<tr><td colspan="3" class="empty-analytics">No click events yet.</td></tr>'; return; }
  body.innerHTML=events.map(event=>`<tr><td>${fmtDate(event.occurredAt)}</td><td>${escapeHtml(event.referrerHost || 'Direct / unknown')}</td><td>${escapeHtml(event.userAgentFamily || 'Unknown')}</td></tr>`).join('');
}

deactivateBtn.addEventListener('click', async () => {
  if(!selectedCode || !confirm(`Deactivate ${selectedCode}?`)) return;
  try {
    await api(`/api/v1/urls/${encodeURIComponent(selectedCode)}`, {method:'DELETE'});
    await loadDetails(selectedCode);
  } catch(e){ alert(e.message); }
});

renderRecent();
refreshRecentStatuses();
setInterval(() => {
  renderRecent(); // local expiry can be derived without a network round trip
}, 15000);
