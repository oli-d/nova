metrics-serialization

---

This artifacts provides the ```MetricsDumpToMapConverter``` class that
can be used to - you might have guessed - convert a ```MetricsDump``` to
a Map.
 
This e.g. is useful if you want to send a dump over the wire, convert
it to JSON or persist it in a data store.

The result ```Map``` contains one entry for each metric contained in the
passed ```MetricsDump```. The entry can be looked up by the metric's name
and each Metric is represented as a (sub) map.

In addition to the all metrics, the result map also contains the following
(root level) entries:
* timestamp
* hostName
* hostAddress