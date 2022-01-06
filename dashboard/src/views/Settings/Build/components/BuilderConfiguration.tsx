import React, {useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Button,
  Box, Select, Divider,
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
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {BuilderConfig, useProvidersInfoQuery} from "../../../../generated/graphql";

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
  providerSelect: {
    width: '150px',
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
    marginLeft: '50px',
    textTransform: 'none'
  }
}));

interface ServicesManagerParams {
  title: string
  builderConfig: BuilderConfig
  services: string[]
  setBuilderConfig: (distribution: string) => void
  removeServiceConfig: (service: string) => void
  setError: (error: string) => void
  error?: string
}

const BuilderConfiguration: React.FC<ServicesManagerParams> = props => {
  const { title, builderConfig, services,
    setBuilderConfig, removeServiceConfig, setError, error } = props

  const [ startEdit, setStartEdit ] = React.useState('')
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

  const classes = useStyles()

  const routeMatch = useRouteMatch()

  const { data: providers } = useProvidersInfoQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query providers info error ' + err.message) },
  })

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

  if (providers?.providersInfo) {
    const distributions = [localStorage.getItem('distribution'), ...providers.providersInfo.map(info => info.distribution)]

    return (
      <Card>
        <CardHeader
          title={title}
        />
        <CardContent>
          <Card>
            <CardHeader
              action={
                <FormGroup row>
                  <FormControlLabel
                    className={classes.control}
                    control={
                      <Select
                        className={classes.providerSelect}
                        native
                        onChange={(event) => {
                          setBuilderConfig(event.target.value as string)
                        }}
                        value={distributions
                          .find(distribution => distribution == builderConfig.distribution)}
                      >
                        {distributions
                          .map((distribution, index) => <option key={index}>{distribution}</option>)}
                      </Select>
                    }
                    label='Distribution Server To Run The Builder'
                  />
                </FormGroup>
              }
              title={'Builder'}
            />
          </Card>
          <Card>
            <CardHeader
              action={
                <Box
                  className={classes.controls}
                >
                  <Button
                    color="primary"
                    className={classes.control}
                    startIcon={<AddIcon/>}
                    title={'Add service'}
                    component={RouterLink}
                    to={`${routeMatch.url}/new`}
                  />
                </Box>
              }
              title={'Services'}
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
        </CardContent>
      </Card>
    )
  } else {
    return null
  }
}

export default BuilderConfiguration