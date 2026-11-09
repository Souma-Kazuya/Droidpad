#!/usr/bin/env python3
"""
Parses Android JUnit XML test reports and generates a GitHub Actions Step Summary.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

def main():
    report_dir = sys.argv[1] if len(sys.argv) > 1 else "android-app/app/build/test-results"

    lines = []
    lines.append("### 🤖 Android Unit Test Results")
    lines.append("")

    if not os.path.exists(report_dir):
        lines.append(f"> ⚠️ Test results directory `{report_dir}` not found (tests may not have run).")
        emit_summary("\n".join(lines))
        return

    xml_files = glob.glob(os.path.join(report_dir, "**", "*.xml"), recursive=True)
    if not xml_files:
        lines.append(f"> ⚠️ No JUnit XML files found in `{report_dir}`.")
        emit_summary("\n".join(lines))
        return

    total_tests = 0
    total_failures = 0
    total_errors = 0
    total_skipped = 0
    total_time = 0.0

    suites = []
    failed_cases = []

    for xml_path in sorted(xml_files):
        try:
            tree = ET.parse(xml_path)
            root = tree.getroot()
        except Exception as e:
            print(f"Warning: Failed to parse XML file {xml_path}: {e}")
            continue

        suite_nodes = [root] if root.tag == "testsuite" else root.findall(".//testsuite")
        for suite in suite_nodes:
            s_name = suite.attrib.get("name", os.path.basename(xml_path))
            s_tests = int(suite.attrib.get("tests", 0))
            s_failures = int(suite.attrib.get("failures", 0))
            s_errors = int(suite.attrib.get("errors", 0))
            s_skipped = int(suite.attrib.get("skipped", 0))
            s_time = float(suite.attrib.get("time", 0.0))

            total_tests += s_tests
            total_failures += s_failures
            total_errors += s_errors
            total_skipped += s_skipped
            total_time += s_time

            s_passed = s_tests - s_failures - s_errors - s_skipped
            suites.append({
                "name": s_name,
                "tests": s_tests,
                "passed": s_passed,
                "failures": s_failures,
                "errors": s_errors,
                "skipped": s_skipped,
                "time": s_time,
            })

            for tc in suite.findall("testcase"):
                tc_name = tc.attrib.get("name", "unknown")
                tc_class = tc.attrib.get("classname", s_name)
                
                failure = tc.find("failure")
                error = tc.find("error")
                issue = failure if failure is not None else error
                if issue is not None:
                    msg = issue.attrib.get("message", "")
                    details = (issue.text or "").strip()
                    failed_cases.append({
                        "class": tc_class,
                        "name": tc_name,
                        "message": msg,
                        "details": details,
                    })

    passed = total_tests - total_failures - total_errors - total_skipped
    is_success = (total_failures == 0 and total_errors == 0 and total_tests > 0)
    status_badge = "✅ Passed" if is_success else ("❌ Failed" if (total_failures > 0 or total_errors > 0) else "⚪ No Tests")

    lines.append(f"**Status**: {status_badge}")
    lines.append("")
    lines.append("| Metric | Count | Status |")
    lines.append("| :--- | :--- | :--- |")
    lines.append(f"| **Total Tests** | {total_tests} | 📊 |")
    lines.append(f"| **Passed** | {passed} | {'✅' if passed > 0 else '⚪'} |")
    lines.append(f"| **Failed** | {total_failures} | {'❌' if total_failures > 0 else '✅'} |")
    lines.append(f"| **Errors** | {total_errors} | {'❌' if total_errors > 0 else '✅'} |")
    lines.append(f"| **Skipped** | {total_skipped} | {'⚪'} |")
    lines.append(f"| **Duration** | {total_time:.2f}s | ⏱️ |")
    lines.append("")

    if suites:
        lines.append("#### Test Suites Breakdown")
        lines.append("")
        lines.append("| Test Suite | Tests | Passed | Failed | Skipped | Time |")
        lines.append("| :--- | :--- | :--- | :--- | :--- | :--- |")
        for s in suites:
            # Shorten suite name if very long
            short_name = s['name'].split('.')[-1]
            status_emoji = "✅" if s['failures'] == 0 and s['errors'] == 0 else "❌"
            lines.append(
                f"| `{short_name}` ({s['name']}) | {s['tests']} | {s['passed']} {status_emoji} | {s['failures']} | {s['skipped']} | {s['time']:.2f}s |"
            )
        lines.append("")

    if failed_cases:
        lines.append("#### ❌ Failed Test Cases:")
        lines.append("")
        for fc in failed_cases:
            lines.append(f"<details><summary><b><code>{fc['class']}.{fc['name']}</code></b> - {fc['message']}</summary>")
            lines.append("")
            lines.append("```text")
            lines.append(fc['details'] if fc['details'] else "No stacktrace recorded.")
            lines.append("```")
            lines.append("</details>")
            lines.append("")

    lines.append("> ℹ️ *Full HTML and XML test reports are available in the workflow artifacts.*")
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
