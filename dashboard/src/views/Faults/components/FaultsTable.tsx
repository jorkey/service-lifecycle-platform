import React, {useEffect, useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import {DistributionFaultReport} from "../../../generated/graphql";
import {GridTableColumnParams, GridTableCellParams} from "../../../common/components/gridTable/GridTableColumn";
import GridTable from "../../../common/components/gridTable/GridTable";
import {Version} from "../../../common";
import {Button} from "@material-ui/core";
import DownloadIcon from '@material-ui/icons/CloudDownload';
import {download} from "../../../common/Download";
import {GridTableRowParams} from "../../../common/components/gridTable/GridTableRow";

const useStyles = makeStyles(theme => ({
  div: {
    display: 'relative'
  },
  timeColumn: {
    minWidth: '250px',
  },
  distributionColumn: {
    minWidth: '100px',
  },
  serviceColumn: {
    minWidth: '100px',
  },
  versionColumn: {
    minWidth: '250px',
  },
  downloadColumn: {
    maxWidth: '50px',
  }
}))

interface FaultsTableParams {
  className: string
  showDistribution?: boolean
  showService?: boolean
  faults: DistributionFaultReport[]
  onSelected: (fault: DistributionFaultReport|undefined) => void
}

export const FaultsTable = (props: FaultsTableParams) => {
  const { className, showDistribution, showService, faults, onSelected } = props

  const [ selected, setSelected ] = useState(-1)

  const classes = useStyles()

  useEffect(() => {
    onSelected(selected!=-1?faults[selected]:undefined)
  }, [selected])

  const columns: GridTableColumnParams[] = [
    {
      name: 'time',
      headerName: 'Fault Time',
      className: classes.timeColumn,
      type: 'date',
    },
    {
      name: 'distribution',
      headerName: 'Distribution',
      className: classes.distributionColumn,
    },
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn,
    },
    {
      name: 'version',
      headerName: 'Version',
      className: classes.versionColumn,
    },
    {
      name: 'download',
      headerName: 'Download',
      className: classes.downloadColumn,
      type: 'elements'
    }
  ].filter(column => showDistribution || column.name != 'distribution')
   .filter(column => showService || column.name != 'service') as GridTableColumnParams[]

  const development = process.env.NODE_ENV === 'development';

  const rows = faults
    .map((fault, row) => (
      new Map<string, GridTableCellParams>([
        ['time', { value: fault.payload.info.time }],
        ['distribution', { value: fault.distribution }],
        ['service', { value: fault.payload.info.service }],
        ['version', { value:
            fault.payload.info.state.version?Version.clientDistributionVersionToString(fault.payload.info.state.version):'' }],
        ['download', { value: [
          <Button key='0'
                  onClick={
                    () => {
                      download(`http://${development?'localhost:8000':window.location.host}/load/fault-report/` + fault.payload.id,
                `fault-${fault.payload.info.service}-${fault.payload.info.time.toString()}`)
                    }
                  }>
            <DownloadIcon/>
          </Button>
        ]}],
        ['select', { value: row == selected }]
      ])))

  return <GridTable
      className={className}
      columns={columns}
      rows={rows}
      onClick={row => setSelected(row)}
  />
}