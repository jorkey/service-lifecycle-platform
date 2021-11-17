export type GridTableColumnValue = string|number|boolean|Date|JSX.Element[]|undefined

export interface GridTableColumnParams {
  name: string,
  headerName?: string,
  className?: string,
  type?: 'checkbox' | 'date' | 'number' | 'elements',
  select?: string[],
  editable?: boolean,
  validate?: (value: GridTableColumnValue, rowNum: number|undefined) => boolean,
  elements?: JSX.Element[]
}
