select
    ne_datetime,
    cell_id,
    metric_name,
    value
from measurements
where ne_datetime between '{{startDate}}' and '{{endDate}}'
  and value is not null
order by ne_datetime, cell_id
