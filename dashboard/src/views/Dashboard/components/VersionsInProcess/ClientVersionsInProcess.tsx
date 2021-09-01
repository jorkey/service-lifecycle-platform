import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card, CardContent, CardHeader, Grid, Typography,
} from '@material-ui/core';
import {
  useClientVersionsInProcessQuery,
} from "../../../../generated/graphql";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {Version} from "../../../../common";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  versionsTable: {
    marginTop: 20
  },
  control: {
    paddingLeft: '10px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  },
}));

const ClientVersionsInProcess = () => {
  const classes = useStyles()

  const [error, setError] = useState<string>()

  const { data: versionsInProcess, refetch: getVersionsInProcess } = useClientVersionsInProcessQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query client versions in process error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <FormGroup row>
            <RefreshControl
              className={classes.control}
              refresh={() => getVersionsInProcess()}
            />
          </FormGroup>
        }
        title='Client Versions In Process'/>
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          {versionsInProcess?.clientVersionsInProcess?(
            <div>
              <CardContent>
                <Grid container spacing={1}>
                  <Grid item md={1} xs={12}>
                    <Typography>Versions</Typography>
                  </Grid>
                  <Grid item md={11} xs={12}>
                    { versionsInProcess?.clientVersionsInProcess.versions?.map(version => (<Typography>{ version.service + ':' +
                        Version.developerDistributionVersionToString(version.version) }</Typography>)) }
                  </Grid>

                  <Grid item md={1} xs={12}>
                    <Typography>Author</Typography>
                  </Grid>
                  <Grid item md={11} xs={12}>
                    <Typography>{versionsInProcess?.clientVersionsInProcess.author}</Typography>
                  </Grid>

                  <Grid item md={1} xs={12}>
                    <Typography>Start</Typography>
                  </Grid>
                  <Grid item md={11} xs={12}>
                    <Typography>{versionsInProcess?.clientVersionsInProcess.startTime.toLocaleString()}</Typography>
                  </Grid>
                </Grid>
              </CardContent>
            </div>
          ):null}
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  );
}

export default ClientVersionsInProcess