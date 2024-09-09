# This script creates the entity/datasource which will referred by the resource.tf file

variable application_name {default=""}

data "newrelic_entity" "Application" {  
  name = var.application_name
  type = "APPLICATION"
  domain = "APM"
}
