import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card, CardContent, CardHeader, Grid, Typography,
} from '@material-ui/core';
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {useTasksQuery} from "../../../../generated/graphql";

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

  const { data: tasksInProcess, refetch: getTasksInProcess } = useTasksQuery({
    variables: { type: 'BuildClientVersions', onlyActive: true },
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
              refresh={() => getTasksInProcess()}
            />
          </FormGroup>
        }
        title='Client Versions In Process'/>
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          {tasksInProcess?.tasks.length?(
            <div>
              <CardContent>
                <Grid container spacing={1}>
                  <Grid item md={1} xs={12}>
                    <Typography>Versions</Typography>
                  </Grid>
                  <Grid item md={11} xs={12}>
                    { tasksInProcess?.tasks?.map(task => (<Typography>{
                        task.parameters.find(p => p.name == 'versions')?.value }</Typography>)) }
                  </Grid>

                  <Grid item md={1} xs={12}>
                    <Typography>Author</Typography>
                  </Grid>
                  <Grid item md={11} xs={12}>
                    { tasksInProcess?.tasks?.map(task => (<Typography>{
                        task.parameters.find(p => p.name == 'author')?.value }</Typography>)) }
                  </Grid>

                  <Grid item md={1} xs={12}>
                    <Typography>Start</Typography>
                  </Grid>
                  <Grid item md={11} xs={12}>
                    { tasksInProcess?.tasks?.map(task => (<Typography>{
                        task.creationTime?.toLocaleString() }</Typography>)) }
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