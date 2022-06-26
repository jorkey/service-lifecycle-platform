import React, {useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, FormControlLabel, Radio, RadioGroup,
} from '@material-ui/core';
import {
  AccountRole, BuildStatus, TimedBuildServiceState,
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
  Unfinished = 'Unfinished',
  History = 'History'
}

const Builds = () => {
  const classes = useStyles()

  const [mode, setMode] = useState<Mode>(Mode.Unfinished)
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

  let states: TimedBuildServiceState[] | undefined = undefined
  if (mode == Mode.Unfinished) {
    states = buildStates?.buildStates.filter(state => state.status == BuildStatus.InProcess || state.status == BuildStatus.Failure)
  } else if (mode == Mode.History) {
    states = buildStatesHistory?.buildStatesHistory.filter(state => state.status != BuildStatus.InProcess)
      .sort((s1, s2) => s1.time > s2.time ? -1 : s1.time < s2.time ? 1 : 0)
  }

  return states ? (
    <Card>
      <CardHeader
        action={
          <FormGroup row>
            <RadioGroup row
                        value={mode}
                        onChange={ event => setMode(event.target.value as Mode) }
            >
              <FormControlLabel value={Mode.Unfinished} control={<Radio/>} label="Unfinished"/>
              <FormControlLabel value={Mode.History} control={<Radio/>} label="History"/>
            </RadioGroup>
            <RefreshControl
              className={classes.control}
              refresh={() => { getBuildStates(); getBuildStatesHistory() } }
            />
          </FormGroup>
        }
        title={mode==Mode.Unfinished?'Unfinished Builds':'Builds History'}
      />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <BuildsTable buildStates={states}/>
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  ) : null;
}

export default Builds