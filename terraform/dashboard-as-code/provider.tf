# This script configures the newrelic-terraform provider using the New Relic account_id and newrelic_api_key

terraform {
  # Require Terraform version 0.13.x (recommended)
  #required_version = "~> 0.13.0"
  required_version = "> 1.0.1"

  # Require the latest 2.x version of the New Relic provider
  required_providers {
    newrelic = {
      source  = "newrelic/newrelic"
      version = "~> 2.21"
    }
  }
}

variable account_id {default=""}

provider "newrelic" {
  account_id = var.account_id
  region = "US"
}
