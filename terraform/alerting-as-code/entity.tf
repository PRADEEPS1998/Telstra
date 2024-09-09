# This script creates the entity/datasource which will referred by the resource.tf file

# Data Source
data "newrelic_entity" "Application" {
  name = lookup(var.nr_account_details,"application_name") # Note: This must be an exact match of your app name in New Relic (Case sensitive)
  type = "APPLICATION"
  domain = "APM"
}
