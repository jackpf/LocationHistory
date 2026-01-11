import React from 'react';
import {Select} from 'antd';
import {TimeRangeOption} from "../utils/time-range-options.ts";

interface TimeRangeSelectorProps {
    value: TimeRangeOption;
    onChange: (value: TimeRangeOption) => void;
}

const PRESETS = [
    {value: TimeRangeOption.LAST_15_MIN, label: 'Last 15 Minutes'},
    {value: TimeRangeOption.LAST_HOUR, label: 'Last Hour'},
    {value: TimeRangeOption.LAST_24_HOURS, label: 'Last 24 Hours'},
    {value: TimeRangeOption.LAST_3_DAYS, label: 'Last 3 Days'},
    {value: TimeRangeOption.LAST_7_DAYS, label: 'Last Week'},
    {value: TimeRangeOption.LAST_30_DAYS, label: 'Last Month'},
    {value: TimeRangeOption.LAST_YEAR, label: 'Last Year'},
];

export const TimeRangeSelector: React.FC<TimeRangeSelectorProps> = ({
                                                                        value,
                                                                        onChange
                                                                    }) => {
    return (
        <Select
            value={value}
            style={{width: 160}}
            onChange={onChange}
            options={PRESETS}
        />
    );
};