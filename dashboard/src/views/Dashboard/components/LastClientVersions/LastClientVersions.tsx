import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader,
} from '@material-ui/core';
import {
  useClientVersionsInfoQuery, useTasksQuery,
} from "../../../../generated/graphql";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import ClientVersionsTable from "./ClientVersionsTable";
import ClientVersionsInProcessTable from "./ClientVersionsInProcessTable";

const useStyles = makeStyles((theme:any) => ({
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
  }
}));

const LastClientVersions = () => {
  const classes = useStyles()

  const { data: clientVersionsInProcess, refetch: getClientVersionsInProcess } = useTasksQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { type: 'BuildClientVersions', onlyActive: true },
    onError(err) { setError('Query client versions in process error ' + err.message) },
  })

  const { data:clientVersions, refetch:getClientVersions } = useClientVersionsInfoQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query client versions error ' + err.message) },
  })

  const [error, setError] = useState<string>()

  return (
    <Card>
      <CardHeader
        action={
          <FormGroup row>
            <RefreshControl
              className={classes.control}
              refresh={() => { getClientVersionsInProcess(); getClientVersions()}}
            />
          </FormGroup>
        }
        title='Last Client Versions'
      />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <ClientVersionsInProcessTable clientVersionsInProcess={clientVersionsInProcess}/>
          <ClientVersionsTable clientVersions={clientVersions}/>
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  );
}

export default LastClientVersions