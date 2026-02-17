# Setup Guide

## 1. Build artifacts locally

```bash
mvn -f paper-plugin/pom.xml clean package
mvn -f backend-quarkus/pom.xml clean package -DskipTests
```

Plugin jar output:

- `paper-plugin/target/minecraft-jira-plugin.jar`

## 2. Create namespace and deploy backend in `minecraft`

```bash
oc new-project minecraft
oc project minecraft
```

Create API key and GitHub token secret (replace placeholders):

```bash
export MINECRAFT_API_KEY="replace-with-strong-random-key"
export GITHUB_TOKEN="replace-with-your-github-token"

oc create secret generic minecraft-backend-secrets \
  --from-literal=MINECRAFT_API_KEY="$MINECRAFT_API_KEY" \
  --from-literal=GITHUB_TOKEN="$GITHUB_TOKEN" \
  -n minecraft
```

Read-only endpoint discovery from `llm-hosting` namespace (do not modify resources there):

```bash
oc get svc -n llm-hosting
oc get route -n llm-hosting
```

Set `LLM_BASE_URL` from discovered route in configmap:

```bash
oc apply -f deploy/openshift/backend-configmap.yaml -n minecraft
```

## 3. Build backend image in-cluster and deploy

Create binary build config:

```bash
oc new-build --binary --strategy=docker --name=minecraft-backend -n minecraft
```

Start build from local source:

```bash
oc start-build minecraft-backend --from-dir=backend-quarkus --follow -n minecraft
```

Deploy backend service + route:

```bash
oc apply -f deploy/openshift/backend-deployment.yaml -n minecraft
oc apply -f deploy/openshift/backend-service.yaml -n minecraft
oc apply -f deploy/openshift/backend-route.yaml -n minecraft
```

Check route URL:

```bash
oc get route minecraft-backend -n minecraft
```

## 4. Configure Paper plugin (Path A: local Paper recommended)

- Copy plugin jar to local Paper server `plugins/` directory.
- Start Paper once to generate config.
- Edit `plugins/MinecraftJira/config.yml`:
  - `backend.base-url`: backend route from OpenShift
  - `backend.api-key`: same value as `MINECRAFT_API_KEY`

Restart Paper.

## 5. Optional Path B: run PaperMC in OpenShift

Apply manifests:

```bash
oc apply -f deploy/openshift/paper-pvc.yaml -n minecraft
oc apply -f deploy/openshift/paper-deployment.yaml -n minecraft
```

Expose port `25565` (choose one):

```bash
oc apply -f deploy/openshift/paper-service-nodeport.yaml -n minecraft
# or
oc apply -f deploy/openshift/paper-service-loadbalancer.yaml -n minecraft
```

Copy plugin jar into running Paper pod:

```bash
PAPER_POD=$(oc get pod -n minecraft -l app=minecraft-paper -o jsonpath='{.items[0].metadata.name}')
oc rsync paper-plugin/target/minecraft-jira-plugin.jar "$PAPER_POD":/data/plugins/ -n minecraft
```

Then edit `/data/plugins/MinecraftJira/config.yml` in pod shell and restart deployment:

```bash
oc rsh -n minecraft "$PAPER_POD"
# edit file, then exit
oc rollout restart deployment/minecraft-paper -n minecraft
```
