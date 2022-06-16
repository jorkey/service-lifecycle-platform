import React, {useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, FormControlLabel, Radio, RadioGroup,
} from '@material-ui/core';
import {
  AccountRole,
  useBuildStatesHistoryQuery, useBuildStatesQuery,
} from "../../../../generated/graphql";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import BuildsTable from "./BuildsTable";

const useStyles = makeStyles((theme:any) => ({
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  control: {
    paddingLeft: '10px',
  },
  alert: {
    marginTop: 25
  }
}));

export enum Mode {
  Active = 'Active',
  History = 'History'
}

const Builds = () => {
  const classes = useStyles()

  const [mode, setMode] = useState<Mode>(Mode.Active)
  const [error, setError] = useState<string>()

  const { data: buildStates, refetch: getBuildStates } = useBuildStatesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query build states error ' + err.message) },
  })

  const { data: buildStatesHistory, refetch: getBuildStatesHistory } = useBuildStatesHistoryQuery({
    variables: { limit: 100 },
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query build states history error ' + err.message) },
  })

  return (
    <Card>
      <CardHeader
        action={
          <FormGroup row>
            <RadioGroup row
                        value={mode}
                        onChange={ event => setMode(event.target.value as Mode) }
            >
              <FormControlLabel value={Mode.Active} control={<Radio/>} label="Active"/>
              <FormControlLabel value={Mode.History} control={<Radio/>} label="History"/>
            </RadioGroup>
            <RefreshControl
              className={classes.control}
              refresh={() => getBuildStatesHistory()}
            />
          </FormGroup>
        }
        title={mode==Mode.Active?'Active Builds':'Builds History'}
      />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          {mode == Mode.Active ? <BuildsTable buildStates={buildStates?.buildStates}/> :
            <BuildsTable buildStates={buildStatesHistory?.buildStatesHistory}/> }
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  );
}

export default Builds