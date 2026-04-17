#!/usr/bin/env python3
"""
MCP Server for Minecraft Commands
Exposes Minecraft plugin commands as MCP tools for AI to use
"""

import asyncio
import json
from typing import Any
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent
import httpx
import os

# Minecraft backend API endpoint
MINECRAFT_BACKEND_URL = os.getenv("MINECRAFT_BACKEND_URL", "http://minecraft-backend:8080")
MINECRAFT_API_KEY = os.getenv("MINECRAFT_API_KEY", "")

app = Server("minecraft-mcp")

@app.list_tools()
async def list_tools() -> list[Tool]:
    """List available Minecraft command tools"""
    return [
        Tool(
            name="git_pr_list",
            description="List all open pull requests from the configured GitHub repository",
            inputSchema={
                "type": "object",
                "properties": {},
                "required": []
            }
        ),
        Tool(
            name="git_pr_review",
            description="Get an AI-powered review of a pull request. Use 'latest' for most recent PR or specify PR number.",
            inputSchema={
                "type": "object",
                "properties": {
                    "pr_number": {
                        "type": "string",
                        "description": "PR number or 'latest' for most recent"
                    }
                },
                "required": ["pr_number"]
            }
        ),
        Tool(
            name="git_set_repo",
            description="Change the GitHub repository being queried. Format: owner/repo",
            inputSchema={
                "type": "object",
                "properties": {
                    "repository": {
                        "type": "string",
                        "description": "Repository in format 'owner/repo'"
                    }
                },
                "required": ["repository"]
            }
        ),
        Tool(
            name="jira_list",
            description="List Jira issues. Filter by 'mine' (assigned to you), 'bugs' (all bugs), or 'all' (all issues)",
            inputSchema={
                "type": "object",
                "properties": {
                    "filter": {
                        "type": "string",
                        "enum": ["mine", "bugs", "all", ""],
                        "description": "Filter type"
                    }
                },
                "required": []
            }
        ),
        Tool(
            name="jira_view",
            description="View details of a specific Jira issue by key (e.g., PROJ-123)",
            inputSchema={
                "type": "object",
                "properties": {
                    "issue_key": {
                        "type": "string",
                        "description": "Jira issue key (e.g., PROJ-123)"
                    }
                },
                "required": ["issue_key"]
            }
        ),
        Tool(
            name="jira_create",
            description="Create a new Jira issue. Type can be 'Bug', 'Task', or 'Story'",
            inputSchema={
                "type": "object",
                "properties": {
                    "issue_type": {
                        "type": "string",
                        "enum": ["Bug", "Task", "Story"],
                        "description": "Type of issue to create"
                    },
                    "summary": {
                        "type": "string",
                        "description": "Brief summary of the issue"
                    },
                    "description": {
                        "type": "string",
                        "description": "Detailed description (optional)"
                    }
                },
                "required": ["issue_type", "summary"]
            }
        ),
        Tool(
            name="code_explain",
            description="Get AI explanation of code from a file in the repository",
            inputSchema={
                "type": "object",
                "properties": {
                    "file_path": {
                        "type": "string",
                        "description": "Path to the file in the repository"
                    }
                },
                "required": ["file_path"]
            }
        ),
    ]

@app.call_tool()
async def call_tool(name: str, arguments: Any) -> list[TextContent]:
    """Execute a Minecraft command tool"""

    # For now, return mock responses
    # In production, this would call the Minecraft backend API

    if name == "git_pr_list":
        return [TextContent(
            type="text",
            text="Mock PR List:\n1. PR #42: Add new feature\n2. PR #43: Fix bug in parser"
        )]

    elif name == "git_pr_review":
        pr_number = arguments.get("pr_number", "latest")
        return [TextContent(
            type="text",
            text=f"Mock PR Review for {pr_number}:\nThe code changes look good. Consider adding more tests."
        )]

    elif name == "git_set_repo":
        repo = arguments.get("repository")
        return [TextContent(
            type="text",
            text=f"Repository changed to: {repo}"
        )]

    elif name == "jira_list":
        filter_type = arguments.get("filter", "")
        return [TextContent(
            type="text",
            text=f"Mock Jira Issues ({filter_type}):\nPROJ-1: Fix login\nPROJ-2: Add tests"
        )]

    elif name == "jira_view":
        issue_key = arguments.get("issue_key")
        return [TextContent(
            type="text",
            text=f"Mock Issue {issue_key}:\nSummary: Sample issue\nStatus: Open"
        )]

    elif name == "jira_create":
        issue_type = arguments.get("issue_type")
        summary = arguments.get("summary")
        description = arguments.get("description", "")
        return [TextContent(
            type="text",
            text=f"Created {issue_type}: {summary}\nKey: PROJ-123"
        )]

    elif name == "code_explain":
        file_path = arguments.get("file_path")
        return [TextContent(
            type="text",
            text=f"Mock code explanation for {file_path}:\nThis file contains the main application logic."
        )]

    else:
        return [TextContent(
            type="text",
            text=f"Unknown tool: {name}"
        )]

async def main():
    """Run the MCP server"""
    async with stdio_server() as (read_stream, write_stream):
        await app.run(
            read_stream,
            write_stream,
            app.create_initialization_options()
        )

if __name__ == "__main__":
    asyncio.run(main())
