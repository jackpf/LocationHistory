import dayjs, {Dayjs} from 'dayjs';

export enum TimeRangeOption {
  LAST_15_MIN = '15m',
  LAST_HOUR = '1h',
  LAST_24_HOURS = '24h',
  LAST_3_DAYS = '3d',
  LAST_7_DAYS = '7d',
  LAST_30_DAYS = '30d',
  LAST_YEAR = '1y',
}

// The Helper: Call this immediately before you fetch data
export const getTimeRangeParams = (option: TimeRangeOption): { start: Dayjs; end: Dayjs } => {
  const now = dayjs();
  switch (option) {
    case TimeRangeOption.LAST_15_MIN: return { start: now.subtract(15, 'minute'), end: now };
    case TimeRangeOption.LAST_HOUR:   return { start: now.subtract(1, 'hour'), end: now };
    case TimeRangeOption.LAST_24_HOURS: return { start: now.subtract(24, 'hour'), end: now };
    case TimeRangeOption.LAST_3_DAYS: return { start: now.subtract(3, 'day'), end: now };
    case TimeRangeOption.LAST_7_DAYS: return { start: now.subtract(7, 'day'), end: now };
    case TimeRangeOption.LAST_30_DAYS: return { start: now.subtract(30, 'day'), end: now };
    case TimeRangeOption.LAST_YEAR:   return { start: now.subtract(1, 'year'), end: now };
    default: return { start: now.subtract(1, 'hour'), end: now };
  }
};