# OpenShift manifests (`minecraft` namespace only)

All manifests in this directory target the `minecraft` namespace.

## Safety rule

Do not apply any manifest to `llm-hosting`.
Read-only discovery from that namespace is allowed only to collect an endpoint URL.

## Files

- `namespace.yaml`: namespace declaration.
- `backend-configmap.yaml`: non-sensitive backend configuration.
- `backend-secret-template.yaml`: example secret shape. Do not apply as-is.
- `backend-deployment.yaml`: Quarkus backend deployment.
- `backend-service.yaml`: backend cluster service.
- `backend-route.yaml`: HTTPS route for backend.
- `paper-pvc.yaml`: storage for in-cluster PaperMC.
- `paper-deployment.yaml`: optional PaperMC deployment.
- `paper-service-nodeport.yaml`: NodePort exposure for Minecraft TCP (25565).
- `paper-service-loadbalancer.yaml`: LoadBalancer exposure for Minecraft TCP (25565).
