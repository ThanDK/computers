import React, { useState } from 'react';
import { 
    format, startOfMonth, endOfMonth, startOfWeek, endOfWeek, eachDayOfInterval, 
    addMonths, subMonths, isSameMonth, isToday, isSameDay, isAfter, isBefore, 
    setMonth, getMonth, setYear, getYear, addYears, subYears 
} from 'date-fns';
import './CustomDatePicker.css';

export function CustomDatePicker({ 
    range, 
    onRangeChange, 
    mode = 'range', 
    showFooter = true 
}) {
    const [currentDisplayDate, setCurrentDisplayDate] = useState(range.from || new Date());
    const [pickerView, setPickerView] = useState('days'); 

    const handleNavigation = (direction) => {
        if (pickerView === 'days') {
            setCurrentDisplayDate(direction === 'prev' ? subMonths(currentDisplayDate, 1) : addMonths(currentDisplayDate, 1));
        } else if (pickerView === 'months') {
            setCurrentDisplayDate(direction === 'prev' ? subYears(currentDisplayDate, 1) : addYears(currentDisplayDate, 1));
        } else if (pickerView === 'years') {
            setCurrentDisplayDate(direction === 'prev' ? subYears(currentDisplayDate, 12) : addYears(currentDisplayDate, 12));
        }
    };

    const handleTitleClick = () => {
        if (pickerView === 'days') setPickerView('months');
        else if (pickerView === 'months') setPickerView('years');
    };

    const handleDayClick = (day) => {
        if (mode === 'single') {
            onRangeChange({ from: day, to: day });
            return;
        }

        // Range mode logic
        if (!range.from || (range.from && range.to)) {
            onRangeChange({ from: day, to: undefined });
        } else {
            if (isAfter(day, range.from)) {
                onRangeChange({ from: range.from, to: day });
            } else {
                onRangeChange({ from: day, to: undefined });
            }
        }
    };
    
    const handleMonthSelect = (monthIndex) => {
        setCurrentDisplayDate(setMonth(currentDisplayDate, monthIndex));
        setPickerView('days');
    };
    
    const handleYearSelect = (year) => {
        setCurrentDisplayDate(setYear(currentDisplayDate, year));
        setPickerView('months');
    };

    const generateCalendarGrid = (month) => {
        const monthStart = startOfMonth(month);
        const monthEnd = endOfMonth(month);
        const gridStart = startOfWeek(monthStart);
        const gridEnd = endOfWeek(monthEnd);
        return eachDayOfInterval({ start: gridStart, end: gridEnd });
    };

    const generateYearGrid = (date) => {
        const year = getYear(date);
        const startYear = year - (year % 12);
        return Array.from({ length: 12 }, (_, i) => startYear + i);
    };

    const calendarDays = generateCalendarGrid(currentDisplayDate);
    const years = generateYearGrid(currentDisplayDate);
    const weekdays = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

    let guidanceText = "Select a start date.";
    if (mode === 'single') guidanceText = "Select a date.";
    else if (range.from && !range.to) guidanceText = "Select an end date.";

    let headerTitle = format(currentDisplayDate, 'MMMM yyyy');
    if (pickerView === 'months') headerTitle = format(currentDisplayDate, 'yyyy');
    if (pickerView === 'years') headerTitle = `${years[0]} - ${years[11]}`;
    
    const today = new Date();

    return (
        <div className="custom-datepicker-container">
            <div className="datepicker-header">
                <button onClick={() => handleNavigation('prev')}>&lt;</button>
                <button className="datepicker-title" onClick={handleTitleClick}>
                    {headerTitle}
                </button>
                <button onClick={() => handleNavigation('next')}>&gt;</button>
            </div>

            {pickerView === 'days' && (
                <>
                    <div className="weekdays-grid">
                        {weekdays.map(day => <div key={day} className="weekday-cell">{day}</div>)}
                    </div>
                    <div className="days-grid">
                        {calendarDays.map((day, index) => {
                            const isPadding = !isSameMonth(day, currentDisplayDate);
                            const isSelectedStart = range.from && isSameDay(day, range.from);
                            const isSelectedEnd = (range.to && isSameDay(day, range.to)) || (range.from && !range.to && isSameDay(day, range.from));
                            const isInRange = range.from && range.to && isAfter(day, range.from) && isBefore(day, range.to);
                            const isStartOfWeek = index % 7 === 0;
                            const isEndOfWeek = index % 7 === 6;

                            const classNames = [
                                'day-cell',
                                isPadding && 'padding-day',
                                isToday(day) && 'today',
                                isSelectedStart && 'selected-start',
                                isSelectedEnd && 'selected-end',
                                isInRange && 'in-range',
                                isStartOfWeek && 'start-of-week',
                                isEndOfWeek && 'end-of-week'
                            ].filter(Boolean).join(' ');

                            return (
                                <button key={index} className={classNames} onClick={() => handleDayClick(day)} disabled={isPadding}>
                                    {format(day, 'd')}
                                </button>
                            );
                        })}
                    </div>
                    {showFooter && (
                        <div className="datepicker-footer">
                            <span>{guidanceText}</span>
                        </div>
                    )}
                </>
            )}

            {pickerView === 'months' && (
                <div className="months-grid">
                    {months.map((month, index) => {
                        const isCurrentDisplayMonth = getMonth(currentDisplayDate) === index;
                        const isTodayMonth = getMonth(today) === index && getYear(today) === getYear(currentDisplayDate);
                        const classNames = ['month-cell', isCurrentDisplayMonth && 'current-month', isTodayMonth && 'today-marker'].filter(Boolean).join(' ');
                        return (
                            <button key={month} className={classNames} onClick={() => handleMonthSelect(index)}>
                                {month}
                            </button>
                        );
                    })}
                </div>
            )}
            
            {pickerView === 'years' && (
                <div className="years-grid">
                    {years.map((year) => {
                        const isCurrentDisplayYear = getYear(currentDisplayDate) === year;
                        const isTodayYear = getYear(today) === year;
                        const classNames = ['year-cell', isCurrentDisplayYear && 'current-year', isTodayYear && 'today-marker'].filter(Boolean).join(' ');
                        return (
                            <button key={year} className={classNames} onClick={() => handleYearSelect(year)}>
                                {year}
                            </button>
                        );
                    })}
                </div>
            )}
        </div>
    );
}