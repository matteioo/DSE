group "default" {
  targets = ["brakenow", "orchestrator", "simulator", "sonar", "spider", "utracked", "whereami", "storefront"]
}

variable "TAG" {
  default = "latest"
}

# Set to e.g. gcr.io/my-project for GKE builds; leave empty for local/Minikube
variable "REGISTRY" {
  default = ""
}

function "img" {
  params = [name]
  result = REGISTRY != "" ? "${REGISTRY}/${name}:${TAG}" : "${name}:${TAG}"
}

target "base-java" {
  context    = "./services"
  dockerfile = "Dockerfile"
}

target "brakenow" {
  inherits = ["base-java"]
  args     = { SERVICE = "brakenow" }
  tags     = [img("brakenow")]
}

target "orchestrator" {
  inherits = ["base-java"]
  args     = { SERVICE = "orchestrator" }
  tags     = [img("orchestrator")]
}

target "simulator" {
  inherits = ["base-java"]
  args     = { SERVICE = "simulator" }
  tags     = [img("simulator")]
}

target "sonar" {
  inherits = ["base-java"]
  args     = { SERVICE = "sonar" }
  tags     = [img("sonar")]
}

target "spider" {
  inherits = ["base-java"]
  args     = { SERVICE = "spider" }
  tags     = [img("spider")]
}

target "utracked" {
  inherits = ["base-java"]
  args     = { SERVICE = "utracked" }
  tags     = [img("utracked")]
}

target "whereami" {
  inherits = ["base-java"]
  args     = { SERVICE = "whereami" }
  tags     = [img("whereami")]
}

target "storefront" {
  context    = "./services/storefront"
  dockerfile = "Dockerfile"
  tags       = [img("storefront")]
}