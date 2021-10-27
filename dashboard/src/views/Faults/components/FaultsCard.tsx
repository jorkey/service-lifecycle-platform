import React, {useEffect, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, Grid, Select,
} from '@material-ui/core';
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {
  DistributionFaultReport,
  useFaultDistributionsQuery, useFaultsEndTimeQuery, useFaultServicesLazyQuery, useFaultsQuery,
  useFaultsStartTimeQuery
} from "../../../generated/graphql";
import {DateTimePicker} from "@material-ui/pickers";
import Alert from "@material-ui/lab/Alert";
import {FaultsTable} from "./FaultsTable";

const useStyles = makeStyles((theme:any) => ({
  root: {
    padding: theme.spacing(2)
  },
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  distributionSelect: {
    marginLeft: '10px',
    width: '150px',
  },
  serviceSelect: {
    marginLeft: '10px',
    width: '150px',
  },
  date: {
    marginLeft: '10px',
    width: '150px'
  },
  control: {
    paddingLeft: '5px',
    paddingRight: '15px',
    textTransform: 'none'
  },
  faultsTable: {
    height: '300px'
  },
  logsTable: {
    height: '250px'
  },
  alert: {
    marginTop: 25
  }
}));

interface FaultsCardParams {
  onSelected: (report: DistributionFaultReport | undefined) => void
}

const FaultsCard: React.FC<FaultsCardParams> = props => {
  const { onSelected } = props

  const classes = useStyles()

  const [distribution, setDistribution] = useState<string>()
  const [service, setService] = useState<string>()
  const [fromTime, setFromTime] = useState<Date>()
  const [toTime, setToTime] = useState<Date>()

  const [error, setError] = useState<string>()

  useEffect(() => {
    getServices({variables: { distribution: distribution }})
  }, [ distribution ])

  const { data: distributions } = useFaultDistributionsQuery({
    onError(err) { setError('Query fault distributions error ' + err.message) },
  })

  const [ getServices, services ] = useFaultServicesLazyQuery({
    onError(err) { setError('Query fault services error ' + err.message) },
  })

  const { data: startTime } = useFaultsStartTimeQuery({
    variables: { distribution: distribution, service: service },
    onCompleted(data) { if (data.faultsStartTime) setFromTime(data.faultsStartTime) },
    onError(err) { setError('Query faults min time error ' + err.message) },
  })

  const { data: endTime } = useFaultsEndTimeQuery({
    variables: { distribution: distribution, service: service },
    onCompleted(data) { if (data.faultsEndTime) setToTime(data.faultsEndTime) },
    onError(err) { setError('Query faults max time error ' + err.message) },
  })

  const { data: faults } = useFaultsQuery({
    variables: { distribution: distribution, service: service, fromTime: fromTime, toTime: toTime },
    onError(err) { setError(err.message) },
  })

  return <Card>
    <CardHeader
      action={
        <>
          <FormGroup row>
            <FormControlLabel
              className={classes.control}
              labelPlacement={'start'}
              disabled={!distributions?.faultDistributions}
              control={
                <Select
                  className={classes.distributionSelect}
                  native
                  onChange={(event) => {
                    setDistribution(event.target.value?event.target.value as string:undefined)
                  }}
                  title='Select distribution'
                  value={distribution}
                >
                  <option key={-1}/>
                  { distributions?.faultDistributions
                      .map((distribution, index) => <option key={index}>{distribution}</option>)}
                </Select>
              }
              label='Distribution'
            />
            <FormControlLabel
              className={classes.control}
              labelPlacement={'start'}
              control={
                <Select
                  className={classes.serviceSelect}
                  native
                  onChange={(event) => {
                    setService(event.target.value?event.target.value as string:undefined)
                  }}
                  title='Select service'
                  value={service}
                >
                  <option key={-1}/>
                  { services?.data?.faultServices
                    .map((service, index) => <option key={index}>{service}</option>)}
                </Select>
              }
              label='Service'
            />
            <FormControlLabel
              className={classes.control}
              labelPlacement={'start'}
              control={
                <DateTimePicker
                  className={classes.date}
                  value={fromTime}
                  minDate={startTime}
                  ampm={false}
                  onChange={(newValue) => {
                    setFromTime(newValue?newValue:undefined)
                  }}
                />
              }
              label='From'
            />
            <FormControlLabel
              className={classes.control}
              labelPlacement={'start'}
              control={
                <DateTimePicker
                  className={classes.date}
                  value={toTime}
                  minDate={startTime}
                  ampm={false}
                  onChange={(newValue) => {
                    setToTime(newValue?newValue:undefined)
                  }}
                />
              }
              label='To'
            />
          </FormGroup>
        </>
      }
      title={'Failures Of Services'}
    />
    <CardContent className={classes.content}>
      <div className={classes.inner}>
        {faults?.faults ?
          <FaultsTable className={classes.faultsTable}
                       showDistribution={!distribution}
                       showService={!service}
                       faults={faults?.faults}
                       onSelected={selected => onSelected(selected)}
          /> : null }
        {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
      </div>
    </CardContent>
  </Card>
}

export default FaultsCard