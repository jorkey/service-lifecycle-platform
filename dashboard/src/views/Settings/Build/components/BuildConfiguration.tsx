import React, {useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Button,
  Box,
} from '@material-ui/core';
import AddIcon from '@material-ui/icons/Add';
import {Redirect, useRouteMatch} from "react-router-dom";
import { NavLink as RouterLink } from 'react-router-dom';
import GridTable from "../../../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import ConfirmDialog from "../../../../common/components/dialogs/ConfirmDialog";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
import {GridTableCellParams} from "../../../../common/components/gridTable/GridTableCell";
import DeleteIcon from "@material-ui/icons/Delete";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
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
  localBuilder: {
    paddingRight: '2px'
  },
  alert: {
    marginTop: 25
  },
  controls: {
    display: 'flex',
    justifyContent: 'flex-end'
  },
  control: {
    marginLeft: '10px',
  }
}));

interface ServicesManagerParams {
  title: string
  services: string[]
  removeServiceConfig: (service: string) => void
  error?: string
}

const BuildConfiguration: React.FC<ServicesManagerParams> = props => {
  const { title, services, removeServiceConfig, error } = props

  const [ startEdit, setStartEdit ] = React.useState('')
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

  const classes = useStyles()

  const routeMatch = useRouteMatch()

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
    .filter(service => !!service)
    .forEach(service => {
      rows.push(
        new Map<string, GridTableCellParams>([
          ['service', { value: service } ],
          ['actions',
            { value: [<Button key='0' onClick={ () => setDeleteConfirm(service) }>
                <DeleteIcon/>
              </Button>]
            }
          ]
        ]))
    })

  return (
    <Card>
      <CardHeader
        action={
          <Box
            className={classes.controls}
          >
            <Button className={classes.control}
                    color="primary"
                    variant="contained"
                    component={RouterLink}
                    to={`${routeMatch.url}/edit`}
            >
              Common Build Settings
            </Button>
            <Button
              color="primary"
              className={classes.control}
              startIcon={<AddIcon/>}
              title={'Add Service'}
              component={RouterLink}
              to={`${routeMatch.url}/new`}
            />
          </Box>
        }
        title={title}
      />
      <CardContent className={classes.content}>
        <GridTable
          className={classes.servicesTable}
          columns={columns}
          rows={rows}
          onClick={(row) =>
            setStartEdit(rows[row].get('service')?.value! as string)
          }
        />
        {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        {deleteConfirm ? (
          <ConfirmDialog
            message={`Do you want to delete service '${deleteConfirm}'?`}
            open={true}
            close={() =>
              setDeleteConfirm('')
            }
            onConfirm={() => {
              removeServiceConfig(deleteConfirm)
              setDeleteConfirm('')
            }}
          />) : null
        }
      </CardContent>
    </Card>
  )
}

export default BuildConfiguration