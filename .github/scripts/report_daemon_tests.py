#!/usr/bin/env python3
"""
Parses cargo test output and generates a GitHub Actions Step Summary.
"""
import os
import re
import sys

def main():
    if len(sys.argv) < 2:
        print("Usage: report_daemon_tests.py <test-output-file> [os-name]")
        sys.exit(1)

    log_path = sys.argv[1]
    os_name = sys.argv[2] if len(sys.argv) > 2 else "Linux"

    if not os.path.exists(log_path):
        summary_md = f"### 🦀 Linux Daemon Test Results ({os_name})\n\n> ⚠️ No test log found at `{log_path}`.\n"
        emit_summary(summary_md)
        return

    with open(log_path, "r", encoding="utf-8", errors="replace") as f:
        content = f.read()

    # Match test results summary line:
    # test result: ok. 31 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s
    res_match = re.search(
        r"test result:\s+([a-zA-Z]+)\.\s+(\d+)\s+passed;\s+(\d+)\s+failed;\s+(\d+)\s+ignored;\s+(\d+)\s+measured;\s+(\d+)\s+filtered out(?:;\s+finished in\s+([0-9\.]+s))?",
        content
    )

    tests_ok = []
    tests_failed = []
    tests_ignored = []

    for line in content.splitlines():
        line_clean = line.strip()
        m_ok = re.match(r"^test\s+([\w:]+)\s+\.\.\.\s+ok$", line_clean)
        if m_ok:
            tests_ok.append(m_ok.group(1))
            continue
        m_fail = re.match(r"^test\s+([\w:]+)\s+\.\.\.\s+FAILED$", line_clean)
        if m_fail:
            tests_failed.append(m_fail.group(1))
            continue
        m_ign = re.match(r"^test\s+([\w:]+)\s+\.\.\.\s+ignored", line_clean)
        if m_ign:
            tests_ignored.append(m_ign.group(1))
            continue

    if res_match:
        overall_status = res_match.group(1)
        passed = int(res_match.group(2))
        failed = int(res_match.group(3))
        ignored = int(res_match.group(4))
        duration = res_match.group(7) or "N/A"
    else:
        passed = len(tests_ok)
        failed = len(tests_failed)
        ignored = len(tests_ignored)
        overall_status = "FAILED" if failed > 0 else ("ok" if passed > 0 else "unknown")
        duration = "N/A"

    status_badge = "✅ Passed" if overall_status.lower() == "ok" and failed == 0 else "❌ Failed"

    lines = []
    lines.append(f"### 🦀 Linux Daemon Test Results ({os_name}) — {status_badge}")
    lines.append("")
    lines.append("| Metric | Count | Status |")
    lines.append("| :--- | :--- | :--- |")
    lines.append(f"| **Passed** | {passed} | {'✅' if passed > 0 else '⚪'} |")
    lines.append(f"| **Failed** | {failed} | {'❌' if failed > 0 else '✅'} |")
    lines.append(f"| **Ignored** | {ignored} | {'⚪'} |")
    lines.append(f"| **Total** | {passed + failed + ignored} | 📊 |")
    lines.append(f"| **Duration** | {duration} | ⏱️ |")
    lines.append("")

    if tests_failed:
        lines.append("#### ❌ Failed Tests:")
        for t in tests_failed:
            lines.append(f"- `{t}`")
        lines.append("")

    lines.append("<details><summary><b>Detailed Cargo Test Log</b> (click to expand)</summary>")
    lines.append("")
    lines.append("```text")
    # Truncate if gigantic to avoid exceeding GitHub summary limit (1024 KB)
    if len(content) > 50000:
        lines.append(content[:50000] + "\n... [log truncated]")
    else:
        lines.append(content.rstrip())
    lines.append("```")
    lines.append("</details>")
    lines.append("")

    emit_summary("\n".join(lines))

def emit_summary(md: str):
    print(md)
    summary_file = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_file:
        with open(summary_file, "a", encoding="utf-8") as f:
            f.write(md + "\n\n")

if __name__ == "__main__":
    main()
