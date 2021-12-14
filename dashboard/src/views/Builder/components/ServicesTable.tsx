import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {
  useDeveloperServicesQuery
} from '../../../generated/graphql';
import DeleteIcon from '@material-ui/icons/Delete';
import {Redirect, useRouteMatch} from 'react-router-dom';
import ConfirmDialog from '../../../common/components/dialogs/ConfirmDialog';
import GridTable from "../../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import {GridTableColumnParams} from "../../../common/components/gridTable/GridTableColumn";
import {Button} from "@material-ui/core";
import {GridTableCellParams} from "../../../common/components/gridTable/GridTableCell";

const useStyles = makeStyles(theme => ({
  servicesTable: {
  },
  serviceColumn: {
    width: '200px',
  },
  actionsColumn: {
    width: '200px',
    paddingRight: '40px',
    textAlign: 'right'
  },
  alert: {
    marginTop: 25
  }
}));

interface ServicesTableParams {
  services: string[]
  removeServiceConfig: (service: string) => void
}

const ServicesTable: React.FC<ServicesTableParams> = props => {
  const { services, removeServiceConfig } = props

  const [ startEdit, setStartEdit ] = React.useState('')
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

  const [error, setError] = useState<string>()

  const classes = useStyles()

  const routeMatch = useRouteMatch();

  if (startEdit) {
    return <Redirect to={`${routeMatch.url}/edit/${startEdit}`}/>
  }

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn
    },
    {
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = new Array<Map<string, GridTableCellParams>>()
  services
    .sort((s1, s2) =>  (s1 > s2 ? 1 : -1))
    .forEach(service => {
      rows.push(
        new Map<string, GridTableCellParams>([
          ['service', { value: service }],
          ['actions',
            { value: [<Button key='0' onClick={ () => setDeleteConfirm(service) }>
              <DeleteIcon/>
            </Button>] }]
        ]))
    })

  return (<>
    <GridTable
      className={classes.servicesTable}
      columns={columns}
      rows={rows}
      onClick={ (row) =>
        setStartEdit(rows[row].get('service')?.value! as string)
      }
    />
    {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete service '${deleteConfirm}'?`}
        open={true}
        close={() => {
          setDeleteConfirm('')
        }}
        onConfirm={() => {
          removeServiceConfig(deleteConfirm)
          setDeleteConfirm('')
        }}
      />) : null }
  </>)
}

export default ServicesTable;