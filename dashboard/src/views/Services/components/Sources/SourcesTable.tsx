import {IconButton, Link, Table, TableBody, TableCell, TableHead, TableRow} from '@material-ui/core';
import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {
  useDeveloperServicesQuery,
  useRemoveServicesProfileMutation,
  useServiceProfilesQuery, useServiceSourcesQuery,
} from '../../../../generated/graphql';
import EditIcon from '@material-ui/icons/Edit';
import DeleteIcon from '@material-ui/icons/Delete';
import {NavLink as RouterLink, useRouteMatch} from 'react-router-dom';
import ConfirmDialog from '../../../../common/ConfirmDialog';

const useStyles = makeStyles(theme => ({
  serviceColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  actionsColumn: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  }
}));

interface ActionsProps {
  profile: string,
  removing: (promise: Promise<void>) => void
}

const Actions: React.FC<ActionsProps> = (props) => {
  const { profile, removing } = props
  const [ deleteConfirm, setDeleteConfirm ] = useState(false)

  const routeMatch = useRouteMatch();

  const [removeProfile] = useRemoveServicesProfileMutation({
    variables: { profile: profile },
    onError(err) { console.log(err) }
  })

  return (
    <>
      <IconButton
        title="Edit"
        component={RouterLink}
        to={`${routeMatch.url}/edit/${profile}`}
      >
        <EditIcon/>
      </IconButton>
      <IconButton
        onClick={() => setDeleteConfirm(true)}
        title="Delete"
      >
        <DeleteIcon/>
      </IconButton>
      <ConfirmDialog
        close={() => { setDeleteConfirm(false) }}
        message={`Do you want to delete service '${profile}'?`}
        onConfirm={() => removing(removeProfile({ variables: { profile: profile } }).then(() => {}))}
        open={deleteConfirm}
      />
    </>)
}

const SourcesTable = () => {
  const [ selected, setSelected ] = React.useState('')
  const { data: services, refetch } = useDeveloperServicesQuery({ fetchPolicy: 'no-cache' })

  const classes = useStyles()

  return (
    <Table stickyHeader>
      <TableHead>
        <TableRow>
          <TableCell className={classes.serviceColumn}>Service</TableCell>
          <TableCell className={classes.actionsColumn}>Actions</TableCell>
        </TableRow>
      </TableHead>
      { services ?
        <TableBody>
          {services.developerServices.map(service =>
            (<TableRow
              hover
              key={service}
              onClick={() => setSelected(service)}
              selected={service===selected}
            >
              <TableCell className={classes.serviceColumn}>{service}</TableCell>
              <TableCell className={classes.actionsColumn}><Actions
                removing={promise => promise.then(() => refetch())}
                profile={service}
              /></TableCell>
            </TableRow>)
          )}
        </TableBody> : null }
    </Table>)
}

export default SourcesTable;