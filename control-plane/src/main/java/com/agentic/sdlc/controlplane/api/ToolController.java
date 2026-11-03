package com.agentic.sdlc.controlplane.api;

import com.agentic.sdlc.controlplane.tool.ToolBroker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {
    private final ToolBroker toolBroker;

    public ToolController(ToolBroker toolBroker) {
        this.toolBroker = toolBroker;
    }

    @GetMapping
    public List<ToolBroker.ToolDescriptor> catalog() {
        return toolBroker.catalog();
    }
}
