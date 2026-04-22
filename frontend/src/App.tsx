import { useState, useEffect, useCallback } from 'react';
import { Schedule, Member, Team, Resource } from './types';
import * as api from './api/scheduleApi';
import Calendar from './components/calendar/Calendar';
import ScheduleModal from './components/schedule/ScheduleModal';
import ScheduleDetail from './components/schedule/ScheduleDetail';
import Toast from './components/ui/Toast';

export default function App() {
  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [resources, setResources] = useState<Resource[]>([]);
  const [loading, setLoading] = useState(true);

  // Modals
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createDate, setCreateDate] = useState<string | null>(null);
  const [selectedSchedule, setSelectedSchedule] = useState<Schedule | null>(null);
  const [editingSchedule, setEditingSchedule] = useState<Schedule | null>(null);

  // Toast
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const loadSchedules = useCallback(async () => {
    try {
      setLoading(true);
      const data = await api.fetchMonthlySchedules(year, month);
      setSchedules(data);
    } catch (e: any) {
      showToast(e.message || '일정을 불러오는데 실패했습니다.', 'error');
    } finally {
      setLoading(false);
    }
  }, [year, month]);

  const loadMeta = useCallback(async () => {
    try {
      const [m, t, r] = await Promise.all([
        api.fetchMembers(),
        api.fetchTeams(),
        api.fetchResources(),
      ]);
      setMembers(m);
      setTeams(t);
      setResources(r);
    } catch (e: any) {
      showToast('메타데이터 로딩 실패', 'error');
    }
  }, []);

  useEffect(() => { loadMeta(); }, [loadMeta]);
  useEffect(() => { loadSchedules(); }, [loadSchedules]);

  const handleCellClick = (date: string) => {
    setCreateDate(date);
    setShowCreateModal(true);
  };

  const handleEventClick = (schedule: Schedule) => {
    setSelectedSchedule(schedule);
  };

  const handleCreateClose = () => {
    setShowCreateModal(false);
    setCreateDate(null);
  };

  const handleCreated = () => {
    handleCreateClose();
    loadSchedules();
    showToast('일정이 등록되었습니다.');
  };

  const handleDetailClose = () => {
    setSelectedSchedule(null);
  };

  const handleEdit = (schedule: Schedule) => {
    setSelectedSchedule(null);
    setEditingSchedule(schedule);
  };

  const handleEditClose = () => {
    setEditingSchedule(null);
  };

  const handleUpdated = () => {
    setEditingSchedule(null);
    loadSchedules();
    showToast('일정이 수정되었습니다.');
  };

  const handleDeleted = () => {
    setSelectedSchedule(null);
    loadSchedules();
    showToast('일정이 삭제되었습니다.');
  };

  const handleConverted = () => {
    setSelectedSchedule(null);
    loadSchedules();
    showToast('반복 일정으로 전환되었습니다.');
  };

  const goPrev = () => {
    if (month === 1) { setYear(y => y - 1); setMonth(12); }
    else setMonth(m => m - 1);
  };

  const goNext = () => {
    if (month === 12) { setYear(y => y + 1); setMonth(1); }
    else setMonth(m => m + 1);
  };

  const goToday = () => {
    setYear(today.getFullYear());
    setMonth(today.getMonth() + 1);
  };

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="app-logo">
          <div className="app-logo-icon">
            <img src="https://vlast.com/wp-content/uploads/2023/12/vlast_logo_wh.svg" alt="VLAST Logo" style={{ height: '22px' }} />
          </div>
          <div>
            <h1>ASTERUM</h1>
            <span>Integrated Scheduler</span>
          </div>
        </div>
        <button className="btn btn-primary" onClick={() => { setCreateDate(null); setShowCreateModal(true); }}>
          + 새 일정
        </button>
      </header>

      <Calendar
        year={year}
        month={month}
        schedules={schedules}
        loading={loading}
        onPrev={goPrev}
        onNext={goNext}
        onToday={goToday}
        onCellClick={handleCellClick}
        onEventClick={handleEventClick}
      />

      {showCreateModal && (
        <ScheduleModal
          mode="create"
          initialDate={createDate}
          members={members}
          teams={teams}
          resources={resources}
          onClose={handleCreateClose}
          onSaved={handleCreated}
          onError={(msg) => showToast(msg, 'error')}
        />
      )}

      {editingSchedule && (
        <ScheduleModal
          mode="edit"
          schedule={editingSchedule}
          members={members}
          teams={teams}
          resources={resources}
          onClose={handleEditClose}
          onSaved={handleUpdated}
          onError={(msg) => showToast(msg, 'error')}
        />
      )}

      {selectedSchedule && (
        <ScheduleDetail
          schedule={selectedSchedule}
          members={members}
          teams={teams}
          resources={resources}
          onClose={handleDetailClose}
          onEdit={handleEdit}
          onDeleted={handleDeleted}
          onConverted={handleConverted}
          onError={(msg) => showToast(msg, 'error')}
        />
      )}

      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
