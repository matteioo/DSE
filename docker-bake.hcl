group "default" {
  targets = ["brakenow", "orchestrator", "simulator", "sonar", "spider", "utracked", "whereami", "storefront"]
}

variable "TAG" {
  default = "latest"
}

target "base-java" {
  context    = "./services"
  dockerfile = "Dockerfile"
}

target "brakenow" {
  inherits = ["base-java"]
  args     = { SERVICE = "brakenow" }
  tags     = ["brakenow:${TAG}"]
}

target "orchestrator" {
  inherits = ["base-java"]
  args     = { SERVICE = "orchestrator" }
  tags     = ["orchestrator:${TAG}"]
}

target "simulator" {
  inherits = ["base-java"]
  args     = { SERVICE = "simulator" }
  tags     = ["simulator:${TAG}"]
}

target "sonar" {
  inherits = ["base-java"]
  args     = { SERVICE = "sonar" }
  tags     = ["sonar:${TAG}"]
}

target "spider" {
  inherits = ["base-java"]
  args     = { SERVICE = "spider" }
  tags     = ["spider:${TAG}"]
}

target "utracked" {
  inherits = ["base-java"]
  args     = { SERVICE = "utracked" }
  tags     = ["utracked:${TAG}"]
}

target "whereami" {
  inherits = ["base-java"]
  args     = { SERVICE = "whereami" }
  tags     = ["whereami:${TAG}"]
}

target "storefront" {
  context    = "./services/storefront"
  dockerfile = "Dockerfile"
  tags       = ["storefront:${TAG}"]
}