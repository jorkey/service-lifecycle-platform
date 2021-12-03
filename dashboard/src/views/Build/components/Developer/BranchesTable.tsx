import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import GridTable from "../../../../common/components/gridTable/GridTable";
import {GridTableColumnParams, GridTableCellParams} from "../../../../common/components/gridTable/GridTableColumn";
import {GridTableRowParams} from "../../../../common/components/gridTable/GridTableRow";

const useStyles = makeStyles(theme => ({
  branchesTable: {
  },
  nameColumn: {
    width: '200px',
  },
  branchColumn: {
    width: '200px',
  },
  actionsColumn: {
    width: '200px',
    paddingRight: '40px',
    textAlign: 'right'
  },
}));

interface SourceTableParams {
  branches: { name: string, branch: string}[]
  editable: boolean
  onBranchesChanged?: (sources: { name: string, branch: string}[]) => void
}

const BranchesTable = (props: SourceTableParams) => {
  const { branches, editable, onBranchesChanged } = props;

  const classes = useStyles();

  const columns: GridTableColumnParams[] = [
    {
      name: 'source',
      headerName: 'Source Name',
      className: classes.nameColumn
    },
    {
      name: 'branch',
      headerName: 'Branch',
      className: classes.branchColumn,
      editable: editable,
      validate: (value, rowNum) => !!value
    },
    {
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = branches.map(source => (
    new Map<string, GridTableCellParams>([
      ['source', { value: source.name }],
      ['branch', { value: source.branch }]
    ])))

  return (<>
    <GridTable
      className={classes.branchesTable}
      columns={columns}
      rows={rows}
      onRowChanged={ (row, values, oldValues) => {
        onBranchesChanged?.(branches.map(branch =>
          (branch.name == values.get('source')!)?{
            name: values.get('source')! as string, branch: values.get('branch')! as string}:branch)) }}
    />
  </>)
}

export default BranchesTable;
