import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {
  useRemoveServicesProfileMutation,
  useServiceProfilesQuery,
} from '../../../../generated/graphql';
import DeleteIcon from '@material-ui/icons/Delete';
import {NavLink as RouterLink, Redirect, useRouteMatch} from 'react-router-dom';
import ConfirmDialog from '../../../../common/components/dialogs/ConfirmDialog';
import GridTable from "../../../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import {GridTableColumnParams, GridTableCellParams} from "../../../../common/components/gridTable/GridTableColumn";
import {GridTableRowParams} from "../../../../common/components/gridTable/GridTableRow";

const useStyles = makeStyles(theme => ({
  profileTable: {
  },
  profileColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  actionsColumn: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  },
  alert: {
    marginTop: 25
  }
}));

const ProfilesTable = () => {
  const [ startEdit, setStartEdit ] = React.useState('')
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

  const [error, setError] = useState<string>()

  const { data: profiles, refetch } = useServiceProfilesQuery({
    onError(err) { setError('Query service profiles error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [ removeProfile ] = useRemoveServicesProfileMutation({
    onError(err) { setError('Remove services profile error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const classes = useStyles()

  const routeMatch = useRouteMatch();

  if (startEdit) {
    return <Redirect to={`${routeMatch.url}/edit/${startEdit}`}/>
  }

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'profile',
      headerName: 'Profile',
      className: classes.profileColumn
    },
    {
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = new Array<Map<string, GridTableCellParams>>()
  if (profiles) {
    [...profiles.serviceProfiles]
      .sort((s1, s2) =>  (s1 > s2 ? 1 : -1))
      .forEach(profile => {
        rows.push(
          new Map<string, GridTableCellParams>([
            ['profile', { value: profile.profile }],
            ['actions', { value: [<DeleteIcon key='0' onClick={ () => setStartEdit(profile.profile) }/>] }]
          ]))
      })
  }

  return (<>
    <GridTable
      className={classes.profileTable}
      columns={columns}
      rows={rows}
      onClick={ (row) =>
        setStartEdit(rows[row].get('profile')?.value! as string) }
    />
    {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete profile '${deleteConfirm}'?`}
        open={true}
        close={() => { setDeleteConfirm('') }}
        onConfirm={() => {
          removeProfile({
            variables: { profile: deleteConfirm }
          }).then(() => refetch())
          setDeleteConfirm('')
        }}
      />) : null }
  </>)
}

export default ProfilesTable;