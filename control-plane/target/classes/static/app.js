const state = {
  scenario: 'greenfield',
  workflow: null,
  activeTab: 'artifacts',
  renderVersion: 0,
  selectedNodeKey: null,
  busy: false,
  toastTimer: null
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];

const scenarios = {
  greenfield: {
    title: 'Build a new URL-shortener capability',
    hint: 'The agents decompose, build in parallel, validate, then stop at governed approval gates.',
    requirement: 'Add single-use short links. A caller can request a link that becomes permanently unavailable after its first successful redirect. Existing multi-use links must preserve current behavior.'
  },
  brownfield: {
    title: 'Safely change existing URL-shortener code',
    hint: 'The agents inspect the current redirect path first, then make the smallest regression-backed change.',
    requirement: 'Production telemetry shows intermittent redirect correctness issues when cache state is stale or Redis is degraded. Diagnose the existing redirect path and produce the smallest safe change that preserves redirect availability without serving expired or deactivated links.'
  },
  ambiguous: {
    title: 'Prove that the agents know when to stop',
    hint: 'The vague requirement must trigger human clarification before planning or implementation continues.',
    requirement: 'Make shortened links more secure for enterprise customers.'
  }
};

async function json(url, opts = {}) {
  const { headers = {}, ...rest } = opts;
  const response = await fetch(url, {
    ...rest,
    headers: { 'Content-Type': 'application/json', ...headers }
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`${response.status} ${text}`);
  }

  if (response.status === 204) return null;
  return response.json();
}

function notify(message, error = false) {
  const toast = $('#toast');
  toast.textContent = message;
  toast.className = `toast${error ? ' error' : ''}`;
  toast.hidden = false;
  clearTimeout(state.toastTimer);
  state.toastTimer = setTimeout(() => { toast.hidden = true; }, 4200);
}

async function loadTargetSystem() {
  try {
    const target = await json('/api/target-system');
    const pill = $('#targetStatus');
    pill.textContent = `URL Shortener ${target.status}`;
    pill.className = `target-pill ${target.status === 'UP' ? 'up' : target.status === 'DEGRADED' ? 'degraded' : 'down'}`;
    $('#targetGit').textContent = target.branch && target.commit ? `${target.branch} @ ${target.commit}` : '';
    if (target.runtimeUrl) $('#openTarget').href = target.runtimeUrl;
  } catch (e) {
    const pill = $('#targetStatus');
    pill.textContent = 'URL Shortener unavailable';
    pill.className = 'target-pill down';
  }
}

function selectScenario(type) {
  state.scenario = type;
  const config = scenarios[type];
  $$('[data-select-scenario]').forEach((button) => {
    button.classList.toggle('active', button.dataset.selectScenario === type);
  });
  $('#scenarioTitle').textContent = config.title;
  $('#scenarioHint').textContent = config.hint;
  $('#requirementInput').value = config.requirement;
  $('#runWorkflowBtn').textContent = `Run ${titleCase(type)}`;
  $('#retryDemoBtn').hidden = type !== 'greenfield';
}

function setBusy(busy) {
  state.busy = busy;
  $('#runWorkflowBtn').disabled = busy;
  $('#retryDemoBtn').disabled = busy;
  $$('[data-select-scenario]').forEach((button) => { button.disabled = busy; });
}

async function startScenario(retryDemo = false) {
  if (state.busy) return;
  const requirement = $('#requirementInput').value.trim();
  if (!requirement) {
    notify('Enter an engineering requirement.', true);
    return;
  }

  setBusy(true);
  try {
    const workflow = await json(`/api/scenarios/${state.scenario}/workflows`, {
      method: 'POST',
      body: JSON.stringify({ autoRun: false, requirementOverride: requirement })
    });

    state.workflow = workflow;
    state.selectedNodeKey = null;
    renderWorkflow(workflow);
    $('#technicalEvidence').hidden = false;
    await refreshEvidence();

    if (retryDemo) await armRetryDemo(workflow.id);
    await executeWorkflow(workflow.id);
  } catch (e) {
    notify(cleanError(e.message), true);
  } finally {
    setBusy(false);
  }
}

async function armRetryDemo(workflowId) {
  await json(`/api/demo/workflows/${workflowId}/failures`, {
    method: 'POST',
    body: JSON.stringify({
      nodeKey: 'requirements',
      category: 'TRANSIENT_INFRASTRUCTURE',
      failures: 1,
      sideEffectsPossible: false,
      message: 'Demo transient dependency failure',
      actor: 'assessment-dashboard'
    })
  });
  notify('Retry demo armed: Requirements will fail once, then recover automatically.');
}

async function executeWorkflow(workflowId) {
  try {
    const workflow = await json(`/api/workflows/${workflowId}/resume`, { method: 'POST' });
    if (state.workflow?.id !== workflowId) return;
    state.workflow = workflow;
    renderWorkflow(workflow);
    await refreshEvidence();
  } catch (e) {
    await refresh();
    notify(cleanError(e.message), true);
  }
}

async function refresh() {
  if (!state.workflow) return;
  const workflowId = state.workflow.id;
  try {
    const workflow = await json(`/api/workflows/${workflowId}`);
    if (state.workflow?.id !== workflowId) return;
    state.workflow = workflow;
    renderWorkflow(workflow);
    await refreshEvidence();
  } catch (e) {
    console.error('Workflow refresh failed', e);
  }
}

function renderWorkflow(workflow) {
  const renderVersion = ++state.renderVersion;
  $('#workflowPanel').hidden = false;
  $('#workflowTitle').textContent = titleCase(workflow.scenario);
  $('#requirement').textContent = workflow.requirement;
  $('#planRevision').textContent = `revision ${workflow.planRevision}`;
  $('#workflowId').textContent = workflow.id;

  const status = $('#workflowStatus');
  status.textContent = humanStatus(workflow.status);
  status.className = 'status-pill';
  if (workflow.status === 'COMPLETED') status.classList.add('completed');
  if (workflow.status === 'FAILED') status.classList.add('failed');
  if (String(workflow.status).startsWith('WAITING_')) status.classList.add('waiting');

  $$('.node').forEach((element) => {
    element.className = element.classList.contains('compact') ? 'node compact' : 'node';
    const small = element.querySelector('small');
    if (small) small.textContent = 'Pending';
  });

  for (const node of workflow.nodes) {
    const element = document.querySelector(`[data-node="${node.key}"]`);
    if (!element) continue;
    element.classList.add(String(node.status).toLowerCase());
    const small = element.querySelector('small');
    if (small) small.textContent = nodeLabel(node);
    element.title = node.lastError || `${prettyNode(node.key)} • ${node.riskLevel || 'LOW'} risk`;
  }

  if (state.selectedNodeKey) renderNodeInsight(workflow, state.selectedNodeKey);
  else $('#nodeInsight').hidden = true;

  void renderHumanAction(workflow, renderVersion);
  renderOutcome(workflow);
}

function nodeLabel(node) {
  if (node.status === 'SUCCEEDED') return node.attempt > 1 ? `Recovered · ${node.attempt} tries` : 'Done';
  if (node.status === 'RUNNING') return 'Running';
  if (node.status === 'WAITING_FOR_APPROVAL') return 'Approval';
  if (node.status === 'WAITING_FOR_CLARIFICATION') return 'Clarify';
  if (node.status === 'FAILED') return node.failureCategory || 'Failed';
  return 'Pending';
}

function renderNodeInsight(workflow, key) {
  const box = $('#nodeInsight');
  const node = workflow.nodes.find((item) => item.key === key);
  if (!node) {
    box.hidden = true;
    return;
  }

  box.hidden = false;
  box.innerHTML = `
    <strong>${escapeHtml(prettyNode(node.key))}</strong>
    <span>${escapeHtml(humanStatus(node.status))} · ${escapeHtml(node.riskLevel || 'LOW')} risk · attempt ${node.attempt}</span>
    ${node.lastError ? `<span class="error">${escapeHtml(node.lastError)}</span>` : ''}
  `;
}

async function renderHumanAction(workflow, renderVersion) {
  const panel = $('#actionArea');
  const box = $('#humanAction');
  const stillCurrent = () => state.workflow?.id === workflow.id && state.renderVersion === renderVersion;

  panel.hidden = true;
  box.innerHTML = '';

  if (workflow.status === 'WAITING_FOR_CLARIFICATION') {
    try {
      const clarifications = await json(`/api/workflows/${workflow.id}/clarifications`);
      if (!stillCurrent()) return;
      const open = clarifications.find((item) => item.status === 'OPEN');
      if (!open) return;

      panel.hidden = false;
      box.innerHTML = `
        <h3>Clarification needed</h3>
        <p>${open.questions.map((q) => escapeHtml(q)).join(' · ')}</p>
        <textarea id="clarificationAnswer">Enterprise security means blocking local/private destination targets and embedded credentials; existing public links remain backward compatible.</textarea>
        <div class="action-buttons"><button id="resolveClarification" class="approve">Clarify & continue</button></div>
      `;

      $('#resolveClarification').onclick = async () => {
        const answer = $('#clarificationAnswer').value.trim();
        if (!answer) return notify('Provide the clarification before continuing.', true);
        const button = $('#resolveClarification');
        button.disabled = true;
        try {
          const response = await json(`/api/workflows/${workflow.id}/clarifications/${open.id}/resolve`, {
            method: 'POST',
            body: JSON.stringify({ answer, resolvedBy: 'reviewer-a', autoResume: false })
          });
          if (state.workflow?.id === workflow.id) {
            state.workflow = response.workflow;
            renderWorkflow(response.workflow);
            await refreshEvidence();
            await executeWorkflow(workflow.id);
          }
        } catch (e) {
          if (String(e.message).startsWith('409')) return refresh();
          notify(cleanError(e.message), true);
          button.disabled = false;
        }
      };
      return;
    } catch (e) {
      console.error(e);
    }
  }

  const waitingNodes = workflow.nodes.filter((node) => node.status === 'WAITING_FOR_APPROVAL');
  if (waitingNodes.length > 0) {
    panel.hidden = false;
    let rendered = 0;
    for (const node of waitingNodes) {
      let approval;
      try {
        approval = await json(`/api/workflows/${workflow.id}/approvals/${node.key}`);
      } catch (e) {
        continue;
      }
      if (!stillCurrent() || approval.status !== 'PENDING') continue;
      rendered++;

      const decisions = approval.decisions || [];
      const actors = new Set(decisions.map((decision) => decision.actor));
      const reviewerA = actors.has('reviewer-a');
      const reviewerB = actors.has('reviewer-b');

      const card = document.createElement('div');
      card.innerHTML = `
        <h3>${escapeHtml(prettyNode(node.key))} approval</h3>
        <p>${escapeHtml(approval.reason || 'Human approval is required before the workflow can continue.')}</p>
        <div class="reviewer-line">
          <span class="reviewer-chip ${reviewerA ? 'done' : ''}">reviewer-a ${reviewerA ? '✓' : 'pending'}</span>
          ${approval.requiredApprovals > 1 ? `<span class="reviewer-chip ${reviewerB ? 'done' : ''}">reviewer-b ${reviewerB ? '✓' : 'pending'}</span>` : ''}
        </div>
        <div class="action-buttons">
          ${reviewerA ? '' : '<button class="approve" data-approve="reviewer-a">Approve</button>'}
          ${approval.requiredApprovals > 1 && !reviewerB ? '<button class="approve" data-approve="reviewer-b">Approve as reviewer-b</button>' : ''}
          <button class="reject" data-reject="${!reviewerA ? 'reviewer-a' : 'reviewer-b'}">Reject</button>
        </div>
      `;

      card.querySelectorAll('[data-approve]').forEach((button) => {
        button.onclick = () => vote(workflow.id, node.key, 'APPROVE', button.dataset.approve, card);
      });
      card.querySelectorAll('[data-reject]').forEach((button) => {
        button.onclick = () => vote(workflow.id, node.key, 'REJECT', button.dataset.reject, card);
      });
      box.appendChild(card);
    }
    if (rendered === 0) {
      panel.hidden = true;
      setTimeout(() => { if (state.workflow?.id === workflow.id) void refresh(); }, 250);
    }
    return;
  }

  if (workflow.status === 'FAILED') {
    const failed = workflow.nodes.find((node) => node.status === 'FAILED');
    panel.hidden = false;
    box.innerHTML = `
      <h3>Workflow stopped</h3>
      <p>${escapeHtml(failed ? `${prettyNode(failed.key)}: ${failed.lastError || failed.failureCategory || 'Execution failed'}` : 'Execution failed.')}</p>
    `;
  }
}

async function vote(workflowId, nodeKey, decision, actor, card) {
  [...card.querySelectorAll('button')].forEach((button) => { button.disabled = true; });
  try {
    const workflow = await json(`/api/workflows/${workflowId}/approvals/${nodeKey}`, {
      method: 'POST',
      body: JSON.stringify({ decision, decidedBy: actor, comment: `${decision} via assessment dashboard` })
    });
    if (state.workflow?.id === workflowId) {
      state.workflow = workflow;
      renderWorkflow(workflow);
      await refreshEvidence();
    }
  } catch (e) {
    if (String(e.message).startsWith('409')) return refresh();
    notify(cleanError(e.message), true);
    await refresh();
  }
}

async function renderOutcome(workflow) {
  const panel = $('#outcomePanel');
  if (workflow.status !== 'COMPLETED') {
    panel.hidden = true;
    return;
  }

  try {
    const summary = await json(`/api/workflows/${workflow.id}/engineering-summary`);
    if (state.workflow?.id !== workflow.id) return;
    const m = summary.metrics;
    $('#outcomeTitle').textContent = 'Review-ready change';
    $('#outcomeText').textContent = `${titleCase(workflow.scenario)} workflow completed against base commit ${workflow.baseCommit}.`;
    $('#outcomeMetrics').innerHTML = [
      ['Nodes', `${m.succeededNodes}/${m.totalNodes}`],
      ['Artifacts', m.artifacts],
      ['Tool runs', m.toolExecutions],
      ['Retries', m.retries]
    ].map(([label, value]) => `<div class="outcome-metric"><small>${label}</small><b>${value}</b></div>`).join('');
    panel.hidden = false;
  } catch (e) {
    panel.hidden = false;
    $('#outcomeMetrics').innerHTML = '';
  }
}

async function refreshEvidence() {
  const workflow = state.workflow;
  if (!workflow) return;
  $('#technicalEvidence').hidden = false;

  try {
    const reliability = await json(`/api/observability/workflows/${workflow.id}`);
    renderReliability(reliability);
  } catch (e) {
    $('#reliability').innerHTML = '';
  }

  try {
    const audit = await json(`/api/workflows/${workflow.id}/audit`);
    $('#audit').innerHTML = audit.length
      ? audit.slice().reverse().slice(0, 70).map((event) => `
          <div class="timeline-item">
            <b>${escapeHtml(event.eventType || event.type || 'EVENT')}</b>
            <small>${escapeHtml(event.nodeKey || 'workflow')} · ${fmtTime(event.createdAt || event.occurredAt || event.timestamp)}</small>
          </div>`).join('')
      : '<div class="timeline-item"><small>No audit events yet.</small></div>';
  } catch (e) {
    $('#audit').innerHTML = '';
  }

  await renderEvidence();
}

function renderReliability(r) {
  const values = [
    ['Success', pct(r.nodeSuccessRate)],
    ['Retries', r.retryCount ?? 0],
    ['Recovered', r.recoveredNodeCount ?? 0],
    ['Replans', r.replanCount ?? 0],
    ['Approvals', r.approvalRequestCount ?? 0],
    ['Artifacts', r.artifactCount ?? 0]
  ];
  $('#reliability').innerHTML = values.map(([label, value]) => `
    <div class="metric"><small>${label}</small><b>${value}</b></div>
  `).join('');

  const story = $('#recoveryStory');
  if ((r.retryCount ?? 0) > 0) {
    story.hidden = false;
    story.textContent = `${r.retryCount} bounded retry event(s); ${r.recoveredNodeCount ?? 0} node(s) recovered automatically${r.meanTimeToRecoveryMs != null ? `; MTTR ${duration(r.meanTimeToRecoveryMs)}` : ''}.`;
  } else {
    story.hidden = true;
  }
}

async function renderEvidence() {
  if (!state.workflow) return;
  const endpoint = state.activeTab === 'artifacts'
    ? 'artifacts'
    : state.activeTab === 'models'
      ? 'model-invocations'
      : 'tool-executions';

  try {
    const items = await json(`/api/workflows/${state.workflow.id}/${endpoint}`);
    $('#evidence').innerHTML = items.length
      ? items.slice().reverse().slice(0, 60).map(renderEvidenceCard).join('')
      : '<div class="evidence-card"><small>No evidence yet.</small></div>';
  } catch (e) {
    $('#evidence').innerHTML = '<div class="evidence-card"><small>Evidence unavailable.</small></div>';
  }
}

function renderEvidenceCard(item) {
  const title = item.type || item.agentRole || item.tool || item.toolName || item.nodeKey || item.status || 'Evidence';
  const detail = item.nodeKey || item.provider || item.requiredCapability || item.capability || item.status || '';
  const hash = item.contentHash || item.responseHash || item.outputHash || item.inputHash || '';
  const preview = item.content ? truncate(item.content, 2600) : '';
  return `
    <div class="evidence-card">
      <b>${escapeHtml(String(title))}</b>
      ${detail ? `<small>${escapeHtml(String(detail))}</small>` : ''}
      ${hash ? `<code>${escapeHtml(String(hash).slice(0, 30))}${String(hash).length > 30 ? '…' : ''}</code>` : ''}
      ${preview ? `<details><summary>Inspect</summary><div class="evidence-preview">${escapeHtml(preview)}</div></details>` : ''}
    </div>
  `;
}

function humanStatus(value) {
  return titleCase(String(value || '').replaceAll('_', ' '));
}
function titleCase(value) {
  return String(value || '').replaceAll('_', ' ').replace(/\b\w/g, (char) => char.toUpperCase());
}
function prettyNode(value) { return titleCase(String(value || '').replaceAll('-', ' ')); }
function pct(value) { return value == null ? '—' : `${Math.round((value <= 1 ? value * 100 : value) * 10) / 10}%`; }
function duration(value) { return value < 1000 ? `${Math.round(value)} ms` : `${(value / 1000).toFixed(2)} s`; }
function fmtTime(value) { try { return value ? new Date(value).toLocaleTimeString() : ''; } catch { return ''; } }
function truncate(value, max) { const text = String(value ?? ''); return text.length <= max ? text : `${text.slice(0, max)}\n…`; }
function cleanError(message) {
  const text = String(message || 'Unexpected error');
  try {
    const firstBrace = text.indexOf('{');
    if (firstBrace >= 0) {
      const data = JSON.parse(text.slice(firstBrace));
      return data.detail || data.title || text;
    }
  } catch (_) {}
  return text;
}
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' })[c]);
}

$$('[data-select-scenario]').forEach((button) => {
  button.onclick = () => selectScenario(button.dataset.selectScenario);
});
$('#runWorkflowBtn').onclick = () => startScenario(false);
$('#retryDemoBtn').onclick = () => startScenario(true);
$('#showEvidenceBtn').onclick = () => {
  $('#technicalEvidence').open = true;
  $('#technicalEvidence').scrollIntoView({ behavior: 'smooth', block: 'start' });
};
$$('.node').forEach((element) => {
  element.onclick = () => {
    state.selectedNodeKey = element.dataset.node;
    if (state.workflow) renderNodeInsight(state.workflow, state.selectedNodeKey);
  };
});
$$('.tab').forEach((button) => {
  button.onclick = () => {
    $$('.tab').forEach((tab) => tab.classList.remove('active'));
    button.classList.add('active');
    state.activeTab = button.dataset.tab;
    void renderEvidence();
  };
});

selectScenario('greenfield');
void loadTargetSystem();
setInterval(() => void loadTargetSystem(), 10000);
setInterval(() => {
  if (!state.workflow) return;
  if (['CREATED', 'RUNNING', 'WAITING_FOR_APPROVAL', 'WAITING_FOR_CLARIFICATION'].includes(state.workflow.status)) {
    void refresh();
  }
}, 1800);
