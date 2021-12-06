import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader,
} from '@material-ui/core';
import {
  useDeveloperVersionsInfoQuery, useTasksQuery,
} from "../../../../generated/graphql";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import DeveloperVersionsTable from "./DeveloperVersionsTable";
import DeveloperVersionsInProcessTable from "./DeveloperVersionsInProcessTable";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  control: {
    paddingLeft: '10px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

const LastDeveloperVersions = () => {
  const classes = useStyles()

  const [error, setError] = useState<string>()

  const { data: developerVersionsInProcess, refetch: getDeveloperVersionsInProcess } = useTasksQuery({
    variables: { type: 'BuildDeveloperVersion', onlyActive: true },
    onError(err) { setError('Query developer versions in process error ' + err.message) },
  })

  const {data:developerVersions, refetch:getDeveloperVersions} = useDeveloperVersionsInfoQuery({
    onError(err) { setError('Query developer versions error ' + err.message) },
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
              refresh={() => getDeveloperVersions()}
            />
          </FormGroup>
        }
        title='Last Developer Versions'
      />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <DeveloperVersionsInProcessTable developerVersionsInProcess={developerVersionsInProcess}/>
          <DeveloperVersionsTable developerVersions={developerVersions}/>
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  );
}

export default LastDeveloperVersions