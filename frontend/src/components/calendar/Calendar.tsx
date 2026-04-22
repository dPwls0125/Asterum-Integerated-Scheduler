import { Schedule } from '../../types';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface CalendarProps {
  year: number;
  month: number;
  schedules: Schedule[];
  loading: boolean;
  onPrev: () => void;
  onNext: () => void;
  onToday: () => void;
  onCellClick: (date: string) => void;
  onEventClick: (schedule: Schedule) => void;
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];
const MONTH_NAMES = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'];

function pad(n: number) { return n.toString().padStart(2, '0'); }

function getCalendarDays(year: number, month: number) {
  const firstDay = new Date(year, month - 1, 1);
  const lastDay = new Date(year, month, 0);
  const startDow = firstDay.getDay(); // 0=Sun
  const daysInMonth = lastDay.getDate();

  const days: { date: string; day: number; otherMonth: boolean }[] = [];

  // Previous month padding
  const prevLastDay = new Date(year, month - 1, 0).getDate();
  for (let i = startDow - 1; i >= 0; i--) {
    const d = prevLastDay - i;
    const m = month - 1 <= 0 ? 12 : month - 1;
    const y = month - 1 <= 0 ? year - 1 : year;
    days.push({ date: `${y}-${pad(m)}-${pad(d)}`, day: d, otherMonth: true });
  }

  // Current month
  for (let d = 1; d <= daysInMonth; d++) {
    days.push({ date: `${year}-${pad(month)}-${pad(d)}`, day: d, otherMonth: false });
  }

  // Next month padding
  const remaining = 7 - (days.length % 7);
  if (remaining < 7) {
    for (let d = 1; d <= remaining; d++) {
      const m = month + 1 > 12 ? 1 : month + 1;
      const y = month + 1 > 12 ? year + 1 : year;
      days.push({ date: `${y}-${pad(m)}-${pad(d)}`, day: d, otherMonth: true });
    }
  }

  return days;
}

export default function Calendar({
  year, month, schedules, loading, onPrev, onNext, onToday, onCellClick, onEventClick
}: CalendarProps) {
  const days = getCalendarDays(year, month);
  const today = new Date();
  const todayStr = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;

  const schedulesByDate: Record<string, Schedule[]> = {};
  schedules.forEach(s => {
    if (!schedulesByDate[s.date]) schedulesByDate[s.date] = [];
    schedulesByDate[s.date].push(s);
  });

  if (loading) {
    return <div className="loading">일정을 불러오는 중...</div>;
  }

  return (
    <div>
      <div className="calendar-header">
        <div className="calendar-nav">
          <button onClick={onPrev}><ChevronLeft size={18} /></button>
          <h2>{year}년 {MONTH_NAMES[month - 1]}</h2>
          <button onClick={onNext}><ChevronRight size={18} /></button>
        </div>
        <button className="btn-today" onClick={onToday}>오늘</button>
      </div>

      <div className="calendar-grid">
        {WEEKDAYS.map((wd, i) => (
          <div key={wd} className="calendar-weekday" style={i === 0 ? { color: 'var(--accent-rose)' } : undefined}>
            {wd}
          </div>
        ))}

        {days.map((cell, i) => {
          const isToday = cell.date === todayStr;
          const isSunday = i % 7 === 0;
          const daySchedules = schedulesByDate[cell.date] || [];
          const maxShow = 3;

          return (
            <div
              key={cell.date + (cell.otherMonth ? '-o' : '')}
              className={`calendar-cell ${cell.otherMonth ? 'other-month' : ''} ${isToday ? 'today' : ''}`}
              onClick={() => onCellClick(cell.date)}
            >
              <div className={`cell-date ${isSunday ? 'sunday' : ''}`}>
                {cell.day}
              </div>
              <div className="cell-events">
                {daySchedules.slice(0, maxShow).map((s, j) => (
                  <div
                    key={j}
                    className={`cell-event ${s.recurring ? 'recurring' : 'onetime'}`}
                    onClick={(e) => { e.stopPropagation(); onEventClick(s); }}
                    title={`${s.startTime?.slice(0, 5)} ${s.title}`}
                  >
                    {s.startTime?.slice(0, 5)} {s.title}
                  </div>
                ))}
                {daySchedules.length > maxShow && (
                  <div className="cell-more">+{daySchedules.length - maxShow}개 더보기</div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
