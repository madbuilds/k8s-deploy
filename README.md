<!--suppress HtmlUnknownAnchorTarget, HtmlDeprecatedAttribute -->
<div id="top"></div>

<div align="center">
  <a href="https://github.com/madbuilds/k8s-deploy/actions">
    <img src="https://img.shields.io/badge/GitHub%20Action-k8s--deploy-blue?logo=kubernetes" alt="k8s-deploy"/>
  </a>
</div>

<h1 align="center">
  k8s-deploy
</h1>

<p align="center">
  GitHub Composite Action for deploying applications to a Kubernetes cluster using kubeconfig, with optional Helm chart support.
</p>

<div align="center">
  🚀 ☸️
</div>

<div align="center">
  <img src="./docs/description.webp" alt="description"/>
</div>

<!-- TABLE OF CONTENT -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#-description">📃 Description</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#-getting-started">🪧 Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li>
      <a href="#%EF%B8%8F-how-to-use">⚠️ How to use</a>
      <ul>
        <li><a href="#inputs">Inputs</a></li>
        <li><a href="#basic-example">Basic Example</a></li>
        <li><a href="#helm-example">Helm Example</a></li>
        <li><a href="#possible-exceptions">Possible Exceptions</a></li>
      </ul>
    </li>
    <li><a href="#-how-it-works">⚙️ How It Works</a></li>
    <li><a href="#-reference">🔗 Reference</a></li>
  </ol>
</details>

<br>

---

## 📃 Description

`k8s-deploy` is a reusable GitHub Composite Action that automates deploying applications to a Kubernetes cluster.  
It supports plain `kubectl` manifests as well as **Helm chart templating**, with built-in RBAC verification and dry-run validation before applying changes.

### Built With

* [kubectl](https://kubernetes.io/docs/reference/kubectl/) — Kubernetes CLI
* [Helm](https://helm.sh/) — Kubernetes package manager
* [azure/setup-kubectl](https://github.com/azure/setup-kubectl) — kubectl installer action
* [azure/setup-helm](https://github.com/azure/setup-helm) — Helm installer action

<p align="right">(<a href="#top">back to top</a>)</p>

## 🪧 Getting Started

### Prerequisites

- A Kubernetes cluster accessible via a valid `kubeconfig`
- Kubernetes manifests or a Helm chart stored in your repository (default path: `.github/k8s/`)
- GitHub Actions enabled in your repository
- A GitHub secret containing your `kubeconfig` content (e.g. `KUBECONFIG`)
- The service account/user in the kubeconfig must have appropriate RBAC permissions (see [How It Works](#%EF%B8%8F-how-it-works))

### Installation

Reference this action from your workflow using the repository path:
```yaml
uses: madbuilds/k8s-deploy@v1
```

No additional installation is needed. The action installs `kubectl` and `helm` automatically during execution.

<p align="right">(<a href="#top">back to top</a>)</p>

## ⚠️ How to use

### Inputs

| Input              | Required | Default         | Description                                                   |
|--------------------|----------|-----------------|---------------------------------------------------------------|
| `kubeconfig`       | ✅ Yes   | —               | Base kubeconfig content used to authenticate to the cluster   |
| `deployment`       | No       | `./.github/k8s/`| Path to the Kubernetes manifests directory or Helm chart root |
| `namespace`        | No       | *(empty)*       | Target namespace to set in the current kubectl context        |
| `context`          | No       | *(empty)*       | Kubectl context name to switch to before deploying            |
| `verify_access`    | No       | `true`          | Run RBAC pre-flight checks before deploying                   |
| `kubectl_version`  | No       | `latest`        | Version of `kubectl` to install                               |
| `helm_version`     | No       | `latest`        | Version of `helm` to install                                  |
| `helm_files`       | No       | *(empty)*       | Newline/space-separated list of Helm values files (`-f`)      |
| `helm_properties`  | No       | *(empty)*       | Newline/space-separated `KEY=VALUE` pairs for `--set`         |

---

### Basic Example

Deploy plain Kubernetes manifests from the default `.github/k8s/` directory:
```yaml
- uses: madbuilds/k8s-deploy@v1
  with:
    kubeconfig: ${{ secrets.KUBECONFIG }}
```

---

### Helm Example

Deploy using a Helm chart with custom values files and property overrides:
```yaml
jobs: 
  deploy: 
    runs-on: ubuntu-latest 
    steps: 
      - uses: actions/checkout@v6
      - uses: madbuilds/k8s-deploy@v1
        with:
          kubeconfig: ${{ secrets.KUBECONFIG }}
          deployment: './.github/k8s/'
          namespace: 'my-namespace'
          context: 'my-cluster-context'
          helm_files: |
            .github/k8s/values.yaml
            .github/k8s/values.prod.yaml
          helm_properties: |
            image.tag=${{ github.sha }}
            replicaCount=3
```
> **Note:** 
> `helm_files`: paths must not contain `../` and must exist in the repository.\
> `helm_properties`: entries must follow the `KEY=VALUE` format.

---

### Possible Exceptions

| Error | Cause | Fix |
|-------|-------|-----|
| `current user is not authorized to <rule>` | RBAC check failed for a required permission | Grant the service account the missing verb/resource permission |
| `invalid values file path (contains ../)` | Helm values file path traversal attempt | Use paths relative to repo root, no `../` allowed |
| `values file not found` | Helm values file path does not exist | Verify the path exists in the repository |
| `invalid --set item (expected KEY=VALUE)` | Malformed `helm_properties` entry | Ensure all entries follow `KEY=VALUE` format |
| API server unreachable | Cluster not reachable within 10s timeout | Check kubeconfig, VPN, or cluster availability |

<p align="right">(<a href="#top">back to top</a>)</p>

## ⚙️ How It Works

The action runs the following steps in order:

1. **Checkout** — checks out the calling repository
2. **Install kubectl** — installs the specified (or latest) `kubectl` version
3. **Install Helm** — installs the specified (or latest) `helm` version
4. **Configure kubectl** — writes kubeconfig to `~/.kube/config`, optionally sets context and namespace
5. **Check connection** — verifies the API server is reachable (`/readyz`)
6. **Verify RBAC** *(optional, enabled by default)* — checks that the authenticated user has all required permissions:
    - `deployments.apps`: get, create, patch, watch
    - `services`: get, create, patch
    - `ingresses.networking.k8s.io`: get, create, patch
    - `configmaps`: get, create, patch
    - `secrets`: get, create, patch
    - `pods`: get, watch
7. **Template Helm chart** *(if `Chart.yaml` + `templates/` exist)* — runs `helm template` with any provided values files and `--set` overrides, outputs `deployment_rendered.yml`
8. **Deploy** — runs client-side dry-run, server-side dry-run, `kubectl diff`, then applies the manifests

<p align="right">(<a href="#top">back to top</a>)</p>

## 🔗 Reference

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [kubectl Reference](https://kubernetes.io/docs/reference/kubectl/)
- [Helm Documentation](https://helm.sh/docs/)
- [azure/setup-kubectl](https://github.com/Azure/setup-kubectl)
- [azure/setup-helm](https://github.com/Azure/setup-helm)
- [GitHub Actions — Composite Actions](https://docs.github.com/en/actions/creating-actions/creating-a-composite-action)
- [GitHub Actions — Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

<p align="right">(<a href="#top">back to top</a>)</p>
