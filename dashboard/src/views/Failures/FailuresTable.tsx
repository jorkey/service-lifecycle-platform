import React from "react";
import {makeStyles} from "@material-ui/core/styles";
import {DistributionFaultReport} from "../../generated/graphql";
import {GridTableColumnParams, GridTableColumnValue} from "../../common/components/gridTable/GridTableColumn";
import GridTable from "../../common/components/gridTable/GridTable";

const useStyles = makeStyles(theme => ({
  div: {
    display: 'relative'
  },
  timeColumn: {
    width: '200px',
    minWidth: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  distributionColumn: {
    width: '100px',
    minWidth: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  serviceColumn: {
    width: '100px',
    minWidth: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  messageColumn: {
    whiteSpace: 'pre',
    padding: '4px',
    paddingLeft: '16px'
  }
}))

interface FailuresTableParams {
  className: string
  showDistribution?: boolean
  showService?: boolean
  faults: DistributionFaultReport[]
}

export const FailuresTable = (props: FailuresTableParams) => {
  const { className, showDistribution, showService, faults } = props

  const classes = useStyles()

  const columns: GridTableColumnParams[] = [
    {
      name: 'time',
      headerName: 'Time',
      className: classes.timeColumn,
      type: 'date',
    },
    {
      name: 'distribution',
      headerName: 'Distribution',
      className: classes.distributionColumn
    },
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn
    },
    {
      name: 'message',
      headerName: 'Line',
      className: classes.messageColumn
    },
  ].filter(column => showDistribution || column.name != 'distribution')
   .filter(column => showService || column.name != 'service')

  const rows = faults
    .map(fault => {
      return new Map<string, GridTableColumnValue>([
        ['time', fault.payload.info.time],
        ['distribution', fault.distribution],
        ['service', fault.payload.info.service],
      ]) })

  return <GridTable
      className={className}
      columns={columns}
      rows={rows}
  />
}