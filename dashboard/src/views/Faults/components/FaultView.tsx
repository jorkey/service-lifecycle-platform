import React, {useEffect, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, Grid, Select,
} from '@material-ui/core';
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {RouteComponentProps} from "react-router-dom";
import {
  DistributionFaultReport,
  useFaultDistributionsQuery, useFaultQuery, useFaultServicesLazyQuery, useFaultsQuery,
  useFaultsStartTimeQuery
} from "../../../generated/graphql";
import {DateTimePicker} from "@material-ui/pickers";
import Alert from "@material-ui/lab/Alert";
import {FaultsTable} from "./FaultsTable";
import TextField from "@material-ui/core/TextField";

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
  control: {
    paddingLeft: '5px',
    paddingRight: '15px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

interface FaultRouteParams {
  id: string
}

interface FaultParams extends RouteComponentProps<FaultRouteParams> {
}

const FaultView: React.FC<FaultParams> = props => {
  const classes = useStyles()

  const [report, setReport] = useState<DistributionFaultReport>()
  const [error, setError] = useState<string>()

  useFaultQuery({
    variables: { id: props.match.params.id },
    fetchPolicy: 'no-cache',
    onCompleted(data) {
      if (data.faults.length) {
        setReport(data.faults[0])
      }
    },
    onError(err) {
      setError('Query fault info error ' + err.message)
    },
  })

  return (report ?
    <div className={classes.root}>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card>
            <CardHeader title={'Fault Info'}/>
            <CardContent className={classes.content}>
              <div className={classes.inner}>
                <TextField
                  autoFocus
                  fullWidth
                  label="Distribution"
                  margin="normal"
                  value={report.distribution}
                  disabled={true}
                  required
                  variant="outlined"
                />
                <TextField
                  autoFocus
                  fullWidth
                  label="Service"
                  margin="normal"
                  value={report.payload.info.service}
                  disabled={true}
                  required
                  variant="outlined"
                />
                {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
              </div>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </div> : null
  )
}

export default FaultView