import React, { useState, useMemo } from 'react';
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  flexRender,
} from '@tanstack/react-table';
import { BsArrowUp, BsArrowDown } from 'react-icons/bs';
import './ReportTable.css';

/**
 * A reusable and sortable table component for displaying report data.
 * @param {{ data: Array<object>, columns: Array<object> }} props
 * - data: The array of data to display.
 * - columns: The column definitions for the table, compatible with @tanstack/react-table.
 */
const ReportTable = ({ data, columns }) => {
  const [sorting, setSorting] = useState([]);

  // Memoize data and columns to prevent unnecessary re-renders, a best practice for this library.
  const memoizedData = useMemo(() => data, [data]);
  const memoizedColumns = useMemo(() => columns, [columns]);

  const table = useReactTable({
    data: memoizedData,
    columns: memoizedColumns,
    state: {
      sorting,
    },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  });

  return (
    <div className="report-table-container">
      <table className="report-table">
        <thead>
          {table.getHeaderGroups().map(headerGroup => (
            <tr key={headerGroup.id}>
              {headerGroup.headers.map(header => (
                <th 
                    key={header.id} 
                    className={header.column.getCanSort() ? 'sortable' : ''}
                    onClick={header.column.getToggleSortingHandler()}
                >
                  {flexRender(
                    header.column.columnDef.header,
                    header.getContext()
                  )}
                  {header.column.getCanSort() && (
                    <span className="sort-indicator">
                      {{
                        asc: <BsArrowUp size={14} />,
                        desc: <BsArrowDown size={14} />,
                      }[header.column.getIsSorted()] ?? null}
                    </span>
                  )}
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody>
          {table.getRowModel().rows.length > 0 ? (
            table.getRowModel().rows.map(row => (
              <tr key={row.id}>
                {row.getVisibleCells().map(cell => (
                  <td key={cell.id}>
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </td>
                ))}
              </tr>
            ))
          ) : (
            <tr className="no-data-row">
              <td colSpan={columns.length}>
                No data available for the selected criteria.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default ReportTable;