# This file creates alert policies,conditions,notification channels with details from terraform.tfvars.json file.
# TODO: Update the notification channels and thresholds as per the requirement in the terraform.tfvars.json file.

variable "notification_channels" {
  type = map(string)
}

variable "transaction_duration_web" {
  type = map(string)
}

variable "transaction_5xx_errors" {
  type = map(string)
}

variable "transaction_4xx_errors" {
  type = map(string)
}

variable "response_time_background_high" {
  type = map(string)
}

variable "high_cpu_utilization" {
  type = map(string)
}

variable "high_memory_utilization" {
  type = map(string)
}

variable "high_failure_rate" {
  type = map(string)
}

variable "low_apdex_score" {
  type = map(string)
}

variable "heap_memory_usage_high" {
  type = map(string)
}

variable "deadlocked_threads" {
  type = map(string)
}

resource "newrelic_alert_policy" "alert_policy_name" {
  name = "${data.newrelic_entity.Application.name}_policy"
}

resource "newrelic_nrql_alert_condition" "nrql_alert_condition_transaction_duration" {
  policy_id                    = newrelic_alert_policy.alert_policy_name.id
  type                         = "static"
  name                         = "High Response Time(Web) Alert"
  description                  = "Alert when transactions are taking too long"
  enabled                      = true
  aggregation_delay            = "120"
  aggregation_method           = "event_flow"
  violation_time_limit_seconds = lookup(var.transaction_duration_web,"violation_time_limit_seconds")

  nrql {
    query             = "SELECT average(duration) FROM Transaction where appName = '${data.newrelic_entity.Application.name}'"
  }

  critical {  
    operator              = "above"
    threshold             = lookup(var.transaction_duration_web,"threshold_value")
    threshold_duration    = lookup(var.transaction_duration_web,"threshold_duration")
    threshold_occurrences = lookup(var.transaction_duration_web,"threshold_occurences")
  }
}

resource "newrelic_nrql_alert_condition" "nrql_alert_condition_transaction_5xx" {
  policy_id                    = newrelic_alert_policy.alert_policy_name.id
  type                         = "static"
  name                         = "Transactions 5xx alert"
  description                  = "Alert when transactions are failing with 5xx"
  enabled                      = true
  aggregation_delay            = "120"
  aggregation_method           = "event_flow"
  violation_time_limit_seconds = lookup(var.transaction_5xx_errors,"violation_time_limit_seconds")

  nrql {
    query             = "SELECT count(*) FROM Transaction where appName = '${data.newrelic_entity.Application.name}' and (httpResponseCode OR http.statusCode) LIKE '5%'  "
  }

  critical {
    operator              = "above"
    threshold             = lookup(var.transaction_5xx_errors,"threshold_value")
    threshold_duration    = lookup(var.transaction_5xx_errors,"threshold_duration")
    threshold_occurrences = lookup(var.transaction_5xx_errors,"threshold_occurences")
  }
}

resource "newrelic_nrql_alert_condition" "nrql_alert_condition_transaction_4xx" {
  policy_id                    = newrelic_alert_policy.alert_policy_name.id
  type                         = "static"
  name                         = "Transactions 4xx alert"
  description                  = "Alert when transactions are failing with 4xx"
  enabled                      = true
  aggregation_delay            = "120"
  aggregation_method           = "event_flow"
  violation_time_limit_seconds = lookup(var.transaction_4xx_errors,"violation_time_limit_seconds")

  nrql {
    query             = "SELECT count(*) FROM Transaction where appName = '${data.newrelic_entity.Application.name}' and (httpResponseCode OR http.statusCode) LIKE '4%'  "
  }

  critical {
    operator              = "above"
    threshold             = lookup(var.transaction_4xx_errors,"threshold_value")
    threshold_duration    = lookup(var.transaction_4xx_errors,"threshold_duration")
    threshold_occurrences = lookup(var.transaction_4xx_errors,"threshold_occurences")
  }
}

resource "newrelic_nrql_alert_condition" "nrql_alert_condition_response_time_background_high" {
  policy_id                    = newrelic_alert_policy.alert_policy_name.id
  type                         = "static"
  name                         = "High Response Time(background process) Condition"
  description                  = "Alert when background processes are taking more time"
  enabled                      = true
  aggregation_delay            = "120"
  aggregation_method           = "event_flow"
  violation_time_limit_seconds = lookup(var.response_time_background_high,"violation_time_limit_seconds")

  nrql {
    query             = "SELECT average(duration) FROM Transaction WHERE appName = '${data.newrelic_entity.Application.name}' AND transactionType!='Web'"
  }

  critical {
    operator              = "above"
    threshold             = lookup(var.response_time_background_high,"threshold_value")
    threshold_duration    = lookup(var.response_time_background_high,"threshold_duration")
    threshold_occurrences = lookup(var.response_time_background_high,"threshold_occurences")
  }
}

resource "newrelic_nrql_alert_condition" "nrql_alert_condition_high_cpu_utilization" {
  policy_id                    = newrelic_alert_policy.alert_policy_name.id
  type                         = "static"
  name                         = "High CPU Utilization Condition"
  description                  = "Alert when CPU Utilization is very high"
  enabled                      = true
  aggregation_delay            = "120"
  aggregation_method           = "event_flow"
  violation_time_limit_seconds = lookup(var.high_cpu_utilization,"violation_time_limit_seconds")

  nrql {
    query             = "SELECT rate(sum(apm.service.cpu.usertime.utilization), 1 second) * 100  as cpuUsage FROM Metric WHERE appName = '${data.newrelic_entity.Application.name}'"
  }

  critical {
    operator              = "above"
    threshold             = lookup(var.high_cpu_utilization,"threshold_value")
    threshold_duration    = lookup(var.high_cpu_utilization,"threshold_duration")
    threshold_occurrences = lookup(var.high_cpu_utilization,"threshold_occurences")
  }
}

resource "newrelic_nrql_alert_condition" "nrql_alert_condition_high_memory_utilization" {
  policy_id                    = newrelic_alert_policy.alert_policy_name.id
  type                         = "static"
  name                         = "High Memory Utilization Condition"
  description                  = "Alert when Memory Utilization is very high"
  enabled                      = true
  aggregation_delay            = "120"
  aggregation_method           = "event_flow"
  violation_time_limit_seconds = lookup(var.high_memory_utilization,"violation_time_limit_seconds")

  nrql {
    query             = "SELECT average(memoryUsedBytes/memoryTotalBytes) * 100 FROM SystemSample where apmApplicationNames like '%${data.newrelic_entity.Application.name}%'"
  }

  critical {
    operator              = "above"
    threshold             = lookup(var.high_memory_utilization,"threshold_value")
    threshold_duration    = lookup(var.high_memory_utilization,"threshold_duration")
    threshold_occurrences = lookup(var.high_memory_utilization,"threshold_occurences")
  }
}

resource "newrelic_nrql_alert_condition" "nrql_alert_condition_high_failure_rate" {
  policy_id                    = newrelic_alert_policy.alert_policy_name.id
  type                         = "static"
  name                         = "High Failure Rate Condition"
  description                  = "Alert when failure rate increase more than threshold"
  enabled                      = true
  aggregation_delay            = "120"
  aggregation_method           = "event_flow"
  violation_time_limit_seconds = lookup(var.high_failure_rate,"violation_time_limit_seconds")

  nrql {
    query             = "SELECT count(apm.service.error.count) / count(apm.service.transaction.duration) * 100 as 'Web errors' FROM Metric WHERE appName = '${data.newrelic_entity.Application.name}' AND (transactionType = 'Web')"
  }

  critical {
    operator              = "above"
    threshold             = lookup(var.high_failure_rate,"threshold_value")
    threshold_duration    = lookup(var.high_failure_rate,"threshold_duration")
    threshold_occurrences = lookup(var.high_failure_rate,"threshold_occurences")
  }
}

resource "newrelic_nrql_alert_condition" "apdex_condition" {
  policy_id                    = newrelic_alert_policy.alert_policy_name.id 
  name                         = "Apdex (Low)"
  description                  = "Alert when apdex score is falling below threshold"
  type                         = "static"
  enabled                      = true
  aggregation_delay            = "120"
  aggregation_method           = "event_flow"
  violation_time_limit_seconds = lookup(var.low_apdex_score,"violation_time_limit_seconds")
  nrql {
    query = "SELECT apdex(duration, t: ${lookup(var.low_apdex_score,"apdex_t")}) FROM Transaction WHERE appName = '${data.newrelic_entity.Application.name}'"
  }
  critical {
    operator              = "below"
    threshold             = lookup(var.low_apdex_score,"threshold_value")
    threshold_duration    = lookup(var.low_apdex_score,"threshold_duration")
    threshold_occurrences = lookup(var.low_apdex_score,"threshold_occurences")
  }
}

resource "newrelic_nrql_alert_condition" "jvm_heap_memory_utilization_condition" {
  policy_id                    = newrelic_alert_policy.alert_policy_name.id 
  name                         = "JVM Heap Utilization (High)"
  description                  = "Alert when JVM Heap memory utilization of the app is high"
  type                         = "static"
  enabled                      = true
  aggregation_delay            = "120"
  aggregation_method           = "event_flow"
  violation_time_limit_seconds = lookup(var.heap_memory_usage_high,"violation_time_limit_seconds")
  critical {
    operator              = "above"
    threshold             = lookup(var.heap_memory_usage_high,"threshold_value")
    threshold_duration    = lookup(var.heap_memory_usage_high,"threshold_duration")
    threshold_occurrences = lookup(var.heap_memory_usage_high,"threshold_occurences")
  }
  nrql {
    query = "SELECT average(newrelic.timeslice.value)*100 AS `Memory/Heap/Utilization percentage` FROM Metric WHERE metricTimesliceName = 'Memory/Heap/Utilization' AND appName='${data.newrelic_entity.Application.name}' FACET `host`"
  }
}


# Deadlocked threads count alert condition
resource "newrelic_nrql_alert_condition" "deadlocked_threads_count_condition" {
  policy_id                    = newrelic_alert_policy.alert_policy_name.id
  name                         = "Deadlocked threads Condition"
  description                  = "Alert when deadlocked threads are present"
  type                         = "static"
  enabled                      = true
  aggregation_delay            = "120"
  aggregation_method           = "event_flow"
  violation_time_limit_seconds = lookup(var.deadlocked_threads,"violation_time_limit_seconds")
  critical {
    operator              = "above_or_equals"
    threshold             = lookup(var.deadlocked_threads,"threshold_value")
    threshold_duration    = lookup(var.deadlocked_threads,"threshold_duration")
    threshold_occurrences = lookup(var.deadlocked_threads,"threshold_occurences")
  }
  nrql {
    query = "SELECT max(newrelic.timeslice.value) * 1000 AS `Threads/Count/New Relic Deadlock Detector/BlockedCount` FROM Metric WHERE metricTimesliceName = 'Threads/Count/New Relic Deadlock Detector/BlockedCount' AND appName='${data.newrelic_entity.Application.name}' FACET `host`"
  }
}

resource "newrelic_alert_channel" "alert_notification_email" {
  count = "${lookup(var.notification_channels,"alert_email_id") != "" ? 1 : 0}"
  name = "Email Channel created for '${data.newrelic_entity.Application.name}'"
  type = "email"

  config {
    recipients              = lookup(var.notification_channels,"alert_email_id")
    include_json_attachment = "1"
  }
}

# Link the above notification channel to your policy
resource "newrelic_alert_policy_channel" "alert_policy_email" {
  #in try block keeping the email[0].type variable as the type value is static.
  count = "${try(newrelic_alert_channel.alert_notification_email[0].type, null) != null ? 1 : 0}"
  policy_id  = newrelic_alert_policy.alert_policy_name.id
  channel_ids = [
    newrelic_alert_channel.alert_notification_email[0].id
  ]
}

resource "newrelic_alert_channel" "pagerduty_alert_channel" {
  count = "${lookup(var.notification_channels,"pagerduty_service_key") != "" ? 1 : 0}"
  name = lookup(var.notification_channels,"pagerduty_service_name")
  type = "pagerduty"

  config {
    service_key = lookup(var.notification_channels,"pagerduty_service_key")
  }
}

resource "newrelic_alert_policy_channel" "alert_policy_pagerduty" {
  #in try block keeping the email[0].type variable as the type value is static.
  count = "${try(newrelic_alert_channel.pagerduty_alert_channel[0].type, null) != null ? 1 : 0}"
  policy_id  = newrelic_alert_policy.alert_policy_name.id
  channel_ids = [
    newrelic_alert_channel.pagerduty_alert_channel[0].id
  ]
}


resource "newrelic_alert_channel" "slack_alert_channel" {
  count = "${lookup(var.notification_channels,"slack_channel_url") != "" ? 1 : 0}"
  name = lookup(var.notification_channels,"slack_channel_name")
  type = "slack"

  config {
    url     = lookup(var.notification_channels,"slack_channel_url")
    channel = lookup(var.notification_channels,"slack_channel_name")
  }
}


resource "newrelic_alert_policy_channel" "alert_policy_slack" {
  #in try block keeping the email[0].type variable as the type value is static.
  count = "${try(newrelic_alert_channel.slack_alert_channel[0].type, null) != null ? 1 : 0}"
  policy_id  = newrelic_alert_policy.alert_policy_name.id
  channel_ids = [
    newrelic_alert_channel.slack_alert_channel[0].id
  ]
}


resource "newrelic_alert_channel" "msteams_alert_channel" {
  count = "${lookup(var.notification_channels,"msteams_channel_link") != "" ? 1 : 0}"
  name = "MS Teams Channel for '${data.newrelic_entity.Application.name}'"
  type = "webhook"

  config {
    base_url = lookup(var.notification_channels,"msteams_channel_link")
    payload_type = "application/json"
    payload_string = <<EOF
{
    "@context": "http://schema.org/extensions",
    "@type": "MessageCard",
    "themeColor": "0072C6",
    "title": "$CONDITION_NAME",
    "text": "$CONDITION_NAME",
    "sections": [
        {
            "activityTitle": "Policy: $POLICY_NAME",
            "activitySubtitle": "State: $EVENT_TYPE",
            "activityText": "Severity: $SEVERITY",
            "facts": [
                {
                    "name": "NewRelic Incident #:",
                    "value": "([$INCIDENT_ID]($INCIDENT_URL)) - $SEVERITY"
                },
                {
                    "name": "Details:",
                    "value": "$EVENT_DETAILS"
                },
                {
                    "name": "Current State:",
                    "value": "$EVENT_STATE"
                },
                {
                    "name": "Account:",
                    "value": "$ACCOUNT_ID $ACCOUNT_NAME"
                }
            ]
        }
    ],
    "potentialAction": [
        {
            "@type": "OpenUri",
            "name": "View Incident",
            "targets": [
                {
                    "os": "default",
                    "uri": "$INCIDENT_URL"
                }
            ]
        },
        {
            "@type": "OpenUri",
            "name": "Acknoweldge Incident",
            "targets": [
                {
                    "os": "default",
                    "uri": "$INCIDENT_ACKNOWLEDGE_URL"
                }
            ]
        },
        {
            "@type": "OpenUri",
            "name": "Runbook",
            "targets": [
                {
                    "os": "default",
                    "uri": "$RUNBOOK_URL"
                }
            ]
        }
    ]
}
EOF
  }
}

resource "newrelic_alert_policy_channel" "alert_policy_msteams" {
  #in try block keeping the email[0].type variable as the type value is static.
  count = "${try(newrelic_alert_channel.msteams_alert_channel[0].type, null) != null ? 1 : 0}"
  policy_id  = newrelic_alert_policy.alert_policy_name.id
  channel_ids = [
    newrelic_alert_channel.msteams_alert_channel[0].id
  ]
}


#Synthetic Monitor alert condition
/* data "newrelic_synthetics_monitor" "monitor_name"{
  name = data.newrelic_entity.Application.name
}

resource "newrelic_synthetics_alert_condition" "synthetic_condition" {
  policy_id = newrelic_alert_policy.alert_policy_name.id

  name        = "Synthetic Monitor failure condition"
  monitor_id  = data.newrelic_synthetics_monitor.monitor_name.id
  runbook_url = "https://www.example.com"
} */
