#!/usr/bin/env bash
# Sets up two local demo git repos and generates application-demo.yml for the backend.
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEMO_DIR="$PROJECT_DIR/demo-repos"
FEATURE_REMOTE="$DEMO_DIR/feature-remote"
DS_REMOTE="$DEMO_DIR/datasource-remote"

echo "=== Setting up demo git repos ==="

for dir in "$FEATURE_REMOTE" "$DS_REMOTE"; do
  if [ ! -d "$dir/.git" ]; then
    git -C "$dir" init
    git -C "$dir" checkout -b main
    git -C "$dir" add .
    git -C "$dir" -c user.email="demo@archvis.local" -c user.name="Demo" commit -m "Initial demo data"
    echo "Initialized: $dir"
  else
    git -C "$dir" add .
    git -C "$dir" -c user.email="demo@archvis.local" -c user.name="Demo" \
      commit -m "Update demo data" 2>/dev/null || echo "(no changes in $dir)"
  fi
done

echo ""
echo "=== Generating application-demo.yml ==="

cat > "$PROJECT_DIR/backend/src/main/resources/application-demo.yml" <<EOF
archvis:
  sync:
    interval-seconds: 300
    local-base-path: ${HOME}/.archvis/repos
  repositories:
    - name: feature-repo-demo
      url: file://${FEATURE_REMOTE}
      branch: main
    - name: datasource-repo-demo
      url: file://${DS_REMOTE}
      branch: main
EOF

echo "Generated: backend/src/main/resources/application-demo.yml"
echo ""
echo "=== Done! ==="
echo ""
echo "Start backend:  cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=demo"
echo "Start frontend: cd frontend && npm install && npm run dev"
echo "API docs:       http://localhost:8080/swagger-ui.html"
echo "Frontend:       http://localhost:5173"
