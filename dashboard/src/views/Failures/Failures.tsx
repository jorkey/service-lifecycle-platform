import React, {useEffect, useRef, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, Checkbox, Grid, Select, TextField,
} from '@material-ui/core';
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {RouteComponentProps} from "react-router-dom";
import {
  useLogServicesQuery
} from "../../generated/graphql";
import {DateTimePicker} from "@material-ui/pickers";

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
    width: '200px'
  },
  info: {
    height: 'calc(100vh - 250px)',
  },
  control: {
    paddingLeft: '5px',
    paddingRight: '15px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

interface FailuresRouteParams {
}

interface FailuresParams extends RouteComponentProps<FailuresRouteParams> {
  fromUrl: string
}

const Failures: React.FC<FailuresParams> = props => {
  const classes = useStyles()

  const [distribution, setDistribution] = useState<string>()
  const [service, setService] = useState<string>()
  const [fromTime, setFromTime] = useState<Date>()
  const [toTime, setToTime] = useState<Date>()

  const [error, setError] = useState<string>()

  useEffect(() => {
    if (service) {
      getInstances({
        variables: { service: service! },
      })
    }
    setInstance(undefined)
    setFromTime(undefined)
    setToTime(undefined)
  }, [ service ])

  useEffect(() => {
    if (service && instance) {
      getDirectories({
        variables: { service: service!, instance: instance! },
      })
    }
    setDirectory(undefined)
  }, [ instance ])

  const { data: services } = useLogServicesQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log services error ' + err.message) },
  })

  const { data: startTime } = useLogStartTimeQuery({
    variables: { service: service, instance: instance, directory: directory, process: process },
    fetchPolicy: 'no-cache',
    onCompleted(data) { if (data.logStartTime) setFromTime(data.logStartTime) },
    onError(err) { setError('Query log min time error ' + err.message) },
  })

  const { data: endTime } = useLogEndTimeQuery({
    variables: { service: service, instance: instance, directory: directory, process: process },
    fetchPolicy: 'no-cache',
    onCompleted(data) { if (data.logEndTime) setToTime(data.logEndTime) },
    onError(err) { setError('Query log max time error ' + err.message) },
  })

  return (
    <div className={classes.root}>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card>
            <CardHeader
              action={
                <>
                  <FormGroup row>
                    <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      disabled={!services?.logServices}
                      control={
                        <Select
                          className={classes.serviceSelect}
                          native
                          onChange={(event) => {
                            setService(event.target.value as string)
                          }}
                          title='Select service'
                          value={service}
                        >
                          <option key={-1}/>
                          { services?.logServices
                              .map((service, index) => <option key={index}>{service}</option>)}
                        </Select>
                      }
                      label='Service'
                    />
                    {!follow ? <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      disabled={!service || instances.loading || !instances.data}
                      control={
                        <DateTimePicker
                          className={classes.date}
                          value={fromTime}
                          minDate={startTime}
                          maxDate={endTime}
                          ampm={false}
                          onChange={(newValue) => {
                            setFromTime(newValue?newValue:undefined)
                          }}
                        />
                      }
                      label='From'
                    /> : null}
                    {!follow ? <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      disabled={!service || instances.loading || !instances.data}
                      control={
                        <DateTimePicker
                          className={classes.date}
                          value={toTime}
                          minDate={startTime}
                          maxDate={endTime}
                          ampm={false}
                          onChange={(newValue) => {
                            setToTime(newValue?newValue:undefined)
                          }}
                        />
                      }
                      label='To'
                    /> : null}
                  </FormGroup>
                </>
              }
              title={'Logs of service'}
            />
            <CardContent className={classes.content}>
              <div className={classes.inner}>
              </div>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </div>
  )
}

export default Failures