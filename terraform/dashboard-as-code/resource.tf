# This file creates dashboards with four pages - Overview, VM details, Errors, Transactions for the given microservice.
# TODO: Developer can choose the widget visualization type of their choice even after the dashboard is created.

resource "newrelic_one_dashboard" "newrelic_java" {
	name = "${data.newrelic_entity.Application.name}_Dashboard"

	permissions = "public_read_write"
	
	page {

		name = "Error/Failure Analysis"
		
		widget_markdown {
			title = ""
			row = 1
			column = 1
			width = 2
			height = 3
			text = "## More details available on Application Monitoring (APM) page.\n\nDive deeper on transaction details, distributed tracing, related entities, anomalies, errors and more. [Open the Explorer.](https://onenr.io/0rVRVGaNWja)"
		}
		
		widget_line {
			title = "Failure Rate"
			row = 1
			column = 3
			width = 4
			height = 3
			nrql_query { 
				query = "SELECT percentage(count(*), WHERE error is true) as 'Failure Rate' FROM Transaction where appName =  '${data.newrelic_entity.Application.name}' timeseries"
			}
		}

		widget_line {
			title = "Throughput"
			row = 1
			column = 7
			width = 3
			height = 3
			nrql_query { 
				query = "FROM Transaction SELECT rate(count(*), 1 minute) where appName =  '${data.newrelic_entity.Application.name}' AND (transactionType = 'Web') timeseries"
			}
		}

		widget_line {
			title = "Response Time"
			row = 1
			column = 10
			width = 3
			height = 3
			nrql_query { 
				query = "SELECT  percentile(duration, 90) FROM Transaction where appName =  '${data.newrelic_entity.Application.name}' AND (transactionType = 'Web') TIMESERIES"
			}
		}

		widget_table {
			title = "Errors Insights"
			row = 4
			column = 1
			width = 6
			height = 4
			nrql_query { 
				query = "From TransactionError select count(*) where appName =  '${data.newrelic_entity.Application.name}' facet transactionName,error.message limit max"
			}
		}

		widget_pie {
			title = "What are the responsecodes?"
			row = 4
			column = 7
			width = 6
			height = 4
			nrql_query { 
				query = "FROM Transaction select count(*) where appName = '${data.newrelic_entity.Application.name}' AND error is true facet httpResponseCode"
			}
		}

		widget_table {
			title = "Error Overview"
			row = 8
			column = 1
			width = 12
			height = 5
			nrql_query { 
				query = "FROM Transaction SELECT count(*) as 'Total transactions',percentage(count(*), WHERE error IS true) as 'Failed transactions (%)', count(*) * percentage(count(*), WHERE error IS true) / 100 as 'Failed transactions' WHERE appName =  '${data.newrelic_entity.Application.name}' FACET name, request.method limit max"
			}
		}
	}
	
	page {
	
		name = "Inbound Analysis"
		
		widget_markdown {
			title = ""
			row = 1
			column = 1
			width = 12
			height = 1
          
            text= "---\n# Response Time Analysis\n---"
        }
		
		widget_billboard {
			title = "Percentiles of Response Times compare with 1 day ago"
			row = 2
			column = 1
			width = 6
			height = 3
			nrql_query { 
				query = "From Transaction select percentile(duration,95,90), average(duration)  where appName =  '${data.newrelic_entity.Application.name}' AND (transactionType = 'Web') since 1 day ago COMPARE WITH 1 day ago "
			}
		}

		widget_line {
			title = "Apdex Analysis (t=0.5)"
			row = 2
			column = 7
			width = 6
			height = 3
			nrql_query { 
				query = "From Transaction select apdex(duration,t:0.5) where appName =  '${data.newrelic_entity.Application.name}' timeseries"
			}
		}

		widget_line {
			title = "Timeseries chart of response time"
			row = 5
			column = 1
			width = 6
			height = 3
			nrql_query { 
				query = "SELECT percentile(duration, 95) , percentile(duration, 99),percentile(duration, 90),  average(duration) as Average FROM Transaction WHERE appName =  '${data.newrelic_entity.Application.name}' AND (transactionType = 'Web') TIMESERIES "
			}
		}

		widget_histogram {
			title = "Histogram of Response Time"
			row = 5
			column = 7
			width = 6
			height = 3
			nrql_query { 
				query = "SELECT histogram(duration, 5, 10) FROM Transaction where appName =  '${data.newrelic_entity.Application.name}' AND transactionType = 'Web'"
			}
		}
		
		widget_markdown {
			title = ""
			row = 8
			column = 1
			width = 12
			height = 1
          
            text= "---\n# Transaction Metrics\n---"
          }
		  
		widget_billboard {
			title = "Overall Transaction count"
			row = 9
			column = 1
			width = 5
			height = 3
			nrql_query { 
				query = "From Transaction select count(*) where appName =  '${data.newrelic_entity.Application.name}' and transactionType = 'Web'"
			}
		}
		
		widget_pie {
			title = "Transaction Count By Name"
			row = 9
			column = 6
			width = 7
			height = 3
			nrql_query { 
				query = "select count(*) FROM Transaction  where appName =  '${data.newrelic_entity.Application.name}' facet name limit max"
			}
		}

		widget_table {
			title = "Top 5 Time consumed Transactions"
			row = 12
			column = 1
			width = 5
			height = 3
			nrql_query { 
				query = "SELECT count(*) * average(duration) AS 'Time Consumed' FROM Transaction WHERE appName =  '${data.newrelic_entity.Application.name}' FACET name LIMIT 5"
			}
		}

		widget_line {
			title = "Average duration of transactions compared with 1 week ago"
			row = 12
			column = 6
			width = 7
			height = 3
			nrql_query { 
				query = "SELECT average(duration) FROM Transaction TIMESERIES WHERE appName =  '${data.newrelic_entity.Application.name}' SINCE today COMPARE WITH 1 week ago"
			}
		}
		widget_table {
			title = "Overview of Transactions"
			row = 15
			column = 1
			width = 12
			height = 6
			nrql_query { 
				query = "FROM Transaction select average(duration) as 'Average', count(*) as 'Total',min(duration) as 'min',max(duration) as 'max',average(duration)*count(*) as 'Total duration' facet name where appName =  '${data.newrelic_entity.Application.name}' limit max"
			}
		}

		widget_markdown {
			title = ""
			row = 21
			column = 1
			width = 12
			height = 1
          
            text= "---\n# Throughput Metrics\n---"
          }
		
		widget_line {
			title = "Inbound - Throughput of APIs (/min)"
			row = 22
			column = 1
			width = 6
			height = 4
			nrql_query { 
				query = "FROM Transaction SELECT rate(count(*), 1 minute) as 'Throughput' where appName =  '${data.newrelic_entity.Application.name}' AND (transactionType = 'Web') timeseries"
			}
		}

		widget_line {
			title = "90th percentile ResponseTime vs Throughput"
			row = 22
			column = 7
			width = 6
			height = 4
			nrql_query { 
				query = "From Transaction select percentile(duration,90) as 'Response Time in sec', rate(count(duration),1 minute)/1000 as 'Throughput in thousands' where appName =  '${data.newrelic_entity.Application.name}' TIMESERIES"
			}
		}
		widget_line {
			title = "Error rate vs Throughput"
			row = 26
			column = 1
			width = 6
			height = 3
			nrql_query { 
				query = "FROM Transaction SELECT rate(count(*), 1 minute)/1000 as 'Throughput in Thousands', percentage(count(*), WHERE error is true) as 'Failure Rate' where appName =  '${data.newrelic_entity.Application.name}'   timeseries"
			}
		}
		widget_billboard {
			title = "Max RPM"
			row = 26
			column = 7
			width = 3
			height = 3
			nrql_query { 
				query = "SELECT max(rpm)/1000 as 'Max RPM(thousands)'\r\nFROM (SELECT count(*) as rpm FROM Transaction WHERE appName =  '${data.newrelic_entity.Application.name}'   TIMESERIES 1 minute)"
			}
		}
		
		widget_billboard {
			title = "Average RPM"
			row = 26
			column = 10
			width = 3
			height = 3
			nrql_query { 
				query = "SELECT average(rpm) as 'Average RPM'\r\nFROM (SELECT count(*) as rpm  FROM Transaction  WHERE appName =  '${data.newrelic_entity.Application.name}' TIMESERIES 1 minute)"
			}
		}
		widget_line {
			title = "Throughput compared with 1 week agao"
			row = 29
			column = 1
			width = 8
			height = 4
			nrql_query { 
				query = "SELECT count(*) from Transaction TIMESERIES 1 hour WHERE appName =  '${data.newrelic_entity.Application.name}' FACET name  since today COMPARE WITH 1 week ago "
			}
		}
	}

	page {
	
		name = "Outbound Analysis"
		
		widget_line {
			title = "Average  ResponseTime of External Services"
			row = 1
			column = 1
			width = 6
			height = 3
			nrql_query { 
				query = "SELECT average(newrelic.timeslice.value) AS `External_Call` FROM Metric WHERE metricTimesliceName Like 'External/%/all' And appName =  '${data.newrelic_entity.Application.name}' facet metricTimesliceName TIMESERIES"
			}
		}
		widget_line {
			title = "Outbound - External Services Throughput "
			row = 1
			column = 7
			width = 6
			height = 3
			nrql_query { 
				query = "SELECT count(newrelic.timeslice.value) AS `External_count` FROM Metric WHERE metricTimesliceName like 'External/%/all' AND appName =  '${data.newrelic_entity.Application.name}' timeseries facet metricTimesliceName"
			}
		}
		widget_line {
			title = "Max Response Time"
			row = 4
			column = 1
			width = 4
			height = 3
			nrql_query { 
				query = "SELECT max(newrelic.timeslice.value) AS `External_Call` FROM Metric WHERE metricTimesliceName Like 'External/%/all' And appName =  '${data.newrelic_entity.Application.name}' facet metricTimesliceName TIMESERIES"
			}
		}
		widget_billboard {
			title = "Outbound - Average ResponseTime of External Service"
			row = 4
			column = 5
			width = 4
			height = 3
			nrql_query { 
				query = "SELECT average(newrelic.timeslice.value) AS `External_Responsetime (sec)` FROM Metric WHERE metricTimesliceName Like 'External/%/all' And appName =  '${data.newrelic_entity.Application.name}' "
			}
		}
		widget_billboard {
			title = "Max External Services Throughput"
			row = 4
			column = 9
			width = 4
			height = 3
			nrql_query { 
				query = "SELECT max(`External_Throughput`) as 'external throughput/min(thousands)'\nFROM (SELECT count(newrelic.timeslice.value) AS `External_Throughput` FROM Metric WHERE metricTimesliceName like 'External/%/all' AND appName =  '${data.newrelic_entity.Application.name}' TIMESERIES 1 minute)"
			}
		}
	}
	
	page {
	
		name = "Infrastructure Metrics"   
		
		    widget_markdown {
			title = ""
			row = 1
			column = 1
			width = 12
			height = 1
            text= "---\n# SRE Signals by Host\n\n---\n"
          }

        widget_line {
            title= "Response Time by Top 20 "
            column= 1
            row= 2
            width= 4
            height= 5
            nrql_query {
                query= "SELECT average(apm.service.transaction.duration) as responseTime FROM Metric WHERE appName =  '${data.newrelic_entity.Application.name}' FACET `host`  timeseries"
              }
        }
        widget_line {
          title= "Throughput by Top 20"
          column= 5
          row= 2
          width=4
          height= 5
          nrql_query {
		      query= "SELECT rate(count(apm.service.transaction.duration), 1 minute) as throughput FROM Metric WHERE appName =  '${data.newrelic_entity.Application.name}' FACET `host` timeseries"
              }
          }

        widget_line {
          title= "Error Rate by Top 20"
          column= 9
          row= 2
          width= 4
          height= 5
          nrql_query {
              query= "SELECT count(apm.service.error.count) / count(apm.service.transaction.duration) as errorRate FROM Metric WHERE appName =  '${data.newrelic_entity.Application.name}' FACET `host` LIMIT 20 TIMESERIES"
              }
            
          }
		  

		  widget_markdown {
			title = ""
			row = 7
			column = 1
			width = 12
			height = 1
            text= "---\n# Host Metrics\n---"
          }


		  widget_bar {
          title= "CPU Utilization %"
          column= 1
          row= 8
          width= 4
          height= 5
          nrql_query {
              query= "SELECT rate(sum(apm.service.cpu.usertime.utilization), 1 second) * 100 as 'CPU Usage %' FROM Metric where appName = '${data.newrelic_entity.Application.name}' FACET `host` LIMIT MAX TIMESERIES"
              }
          }

		  widget_bar {
          title= "Memory Usage %"
          column= 5
          row= 8
          width= 4
          height= 5
          nrql_query {
              query= "SELECT average(apm.service.memory.physical) * rate(count(apm.service.instance.count), 1 minute) / 1000 as 'Memory Usage in GB' FROM Metric WHERE appName= '${data.newrelic_entity.Application.name}' FACET `host` LIMIT MAX TIMESERIES"
              }
          }

          widget_bar {
          title= "JVM Heap Utilization %"
          column= 9
          row= 8
          width= 4
          height= 5
          nrql_query {
              query= "SELECT average(newrelic.timeslice.value)*100 AS `Memory/Heap/Utilization percentage` FROM Metric WHERE metricTimesliceName = 'Memory/Heap/Utilization' AND appName='${data.newrelic_entity.Application.name}' FACET `host` LIMIT MAX TIMESERIES"
              }
          }

	}

	}
