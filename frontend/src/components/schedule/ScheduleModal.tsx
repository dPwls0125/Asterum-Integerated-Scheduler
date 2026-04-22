import { useState } from 'react';
import { Schedule, Member, Team, Resource, ScheduleCreateRequest, ScheduleUpdateRequest, EditScope } from '../../types';
import * as api from '../../api/scheduleApi';
import EditScopeDialog from './EditScopeDialog';
import { X, Repeat } from 'lucide-react';

interface Props {
  mode: 'create' | 'edit';
  schedule?: Schedule | null;
  initialDate?: string | null;
  members: Member[];
  teams: Team[];
  resources: Resource[];
  onClose: () => void;
  onSaved: () => void;
  onError: (msg: string) => void;
}

export default function ScheduleModal({ mode, schedule, initialDate, members, teams, resources, onClose, onSaved, onError }: Props) {
  const isEdit = mode === 'edit' && schedule;

  const [title, setTitle] = useState(isEdit ? schedule!.title : '');
  const [description, setDescription] = useState(isEdit ? (schedule!.description || '') : '');
  const [startDate, setStartDate] = useState(isEdit ? schedule!.date : (initialDate || new Date().toISOString().slice(0, 10)));
  const [startTime, setStartTime] = useState(isEdit ? schedule!.startTime.slice(0, 5) : '09:00');
  const [endTime, setEndTime] = useState(isEdit ? schedule!.endTime.slice(0, 5) : '10:00');
  const [selectedMembers, setSelectedMembers] = useState<number[]>(
    isEdit ? schedule!.participants.map(p => p.id) : []
  );
  const [selectedTeams, setSelectedTeams] = useState<number[]>([]);
  const [resourceId, setResourceId] = useState<number | null>(
    isEdit ? (schedule!.resource?.id ?? null) : null
  );

  // Recurrence (create only)
  const [isRecurring, setIsRecurring] = useState(false);
  const [recType, setRecType] = useState<'DAILY' | 'WEEKLY' | 'MONTHLY'>('WEEKLY');
  const [endType, setEndType] = useState<'NEVER' | 'UNTIL_DATE' | 'COUNT'>('NEVER');
  const [endDate, setEndDate] = useState('');
  const [endCount, setEndCount] = useState(10);

  // Scope dialog for recurring edits
  const [showScope, setShowScope] = useState(false);

  const [saving, setSaving] = useState(false);

  const toggleMember = (id: number) => {
    setSelectedMembers(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  };

  const toggleTeam = (teamId: number) => {
    const team = teams.find(t => t.id === teamId);
    if (!team?.members) return;
    setSelectedTeams(prev => {
      if (prev.includes(teamId)) {
        const teamMemberIds = team.members!.map(m => m.id);
        setSelectedMembers(sm => sm.filter(id => !teamMemberIds.includes(id)));
        return prev.filter(x => x !== teamId);
      } else {
        const teamMemberIds = team.members!.map(m => m.id);
        setSelectedMembers(sm => [...new Set([...sm, ...teamMemberIds])]);
        return [...prev, teamId];
      }
    });
  };

  const handleSubmit = async () => {
    if (!title.trim()) { onError('제목을 입력해주세요.'); return; }
    if (startTime >= endTime) { onError('종료 시간은 시작 시간 이후여야 합니다.'); return; }

    if (isEdit && schedule!.recurring) {
      setShowScope(true);
      return;
    }

    await doSave();
  };

  const doSave = async (scope?: EditScope) => {
    setSaving(true);
    try {
      if (mode === 'create') {
        const req: ScheduleCreateRequest = {
          title, description: description || undefined,
          startDate, startTime, endTime,
          participantIds: selectedMembers.length > 0 ? selectedMembers : undefined,
          teamIds: selectedTeams.length > 0 ? selectedTeams : undefined,
          resourceId: resourceId,
          recurrence: isRecurring ? {
            type: recType, endType,
            endDate: endType === 'UNTIL_DATE' ? endDate : undefined,
            endCount: endType === 'COUNT' ? endCount : undefined,
          } : undefined,
        };
        await api.createSchedule(req);
      } else if (isEdit) {
        const req: ScheduleUpdateRequest = {
          title, description: description || undefined,
          startTime, endTime,
          participantIds: selectedMembers,
          resourceId: resourceId,
        };

        if (schedule!.recurring && schedule!.recurrenceGroupId) {
          await api.updateRecurringInstance(
            schedule!.recurrenceGroupId, schedule!.date, scope || 'THIS', req
          );
        } else if (schedule!.id) {
          await api.updateOneTimeSchedule(schedule!.id, req);
        }
      }
      onSaved();
    } catch (e: any) {
      onError(e.message || '저장에 실패했습니다.');
    } finally {
      setSaving(false);
      setShowScope(false);
    }
  };

  if (showScope) {
    return (
      <EditScopeDialog
        onSelect={(scope) => doSave(scope)}
        onClose={() => setShowScope(false)}
      />
    );
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{mode === 'create' ? '새 일정 등록' : '일정 수정'}</h3>
          <button className="modal-close" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="modal-body">
          <div className="form-group">
            <label className="form-label">제목</label>
            <input className="form-input" value={title} onChange={e => setTitle(e.target.value)}
                   placeholder="촬영, 회의, 연습 등" autoFocus />
          </div>

          <div className="form-group">
            <label className="form-label">설명</label>
            <textarea className="form-textarea" value={description}
                      onChange={e => setDescription(e.target.value)} placeholder="상세 내용 (선택)" />
          </div>

          {mode === 'create' && (
            <div className="form-group">
              <label className="form-label">날짜</label>
              <input className="form-input" type="date" value={startDate}
                     onChange={e => setStartDate(e.target.value)} />
            </div>
          )}

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">시작 시간</label>
              <input className="form-input" type="time" value={startTime}
                     onChange={e => setStartTime(e.target.value)} />
            </div>
            <div className="form-group">
              <label className="form-label">종료 시간</label>
              <input className="form-input" type="time" value={endTime}
                     onChange={e => setEndTime(e.target.value)} />
            </div>
          </div>

          {/* Resource */}
          <div className="form-group">
            <label className="form-label">장소</label>
            <select className="form-select" value={resourceId ?? ''}
                    onChange={e => setResourceId(e.target.value ? Number(e.target.value) : null)}>
              <option value="">장소 선택 (선택사항)</option>
              {resources.map(r => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </select>
          </div>

          {/* Participants */}
          <div className="form-group">
            <label className="form-label">팀 단위 선택</label>
            <div className="participant-grid">
              {teams.map(t => (
                <span key={t.id}
                      className={`team-badge ${selectedTeams.includes(t.id) ? 'selected' : ''}`}
                      onClick={() => toggleTeam(t.id)}>
                  {t.name}
                </span>
              ))}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">참여자</label>
            <div className="participant-grid">
              {members.map(m => (
                <span key={m.id}
                      className={`participant-chip ${m.role === 'ARTIST' ? 'artist' : ''} ${selectedMembers.includes(m.id) ? 'selected' : ''}`}
                      onClick={() => toggleMember(m.id)}>
                  <span className="dot" />
                  {m.name}
                  {m.position ? ` · ${m.position}` : ''}
                </span>
              ))}
            </div>
          </div>

          {/* Recurrence (create only) */}
          {mode === 'create' && (
            <div className="form-group">
              <div className={`recurrence-toggle ${isRecurring ? 'active' : ''}`}
                   onClick={() => setIsRecurring(!isRecurring)}>
                <Repeat size={16} />
                <span style={{ fontSize: '0.85rem' }}>반복 일정으로 등록</span>
              </div>

              {isRecurring && (
                <div className="recurrence-settings">
                  <div className="form-group">
                    <label className="form-label">반복 주기</label>
                    <select className="form-select" value={recType}
                            onChange={e => setRecType(e.target.value as any)}>
                      <option value="DAILY">매일</option>
                      <option value="WEEKLY">매주</option>
                      <option value="MONTHLY">매월</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">종료 조건</label>
                    <select className="form-select" value={endType}
                            onChange={e => setEndType(e.target.value as any)}>
                      <option value="NEVER">무기한 반복</option>
                      <option value="UNTIL_DATE">특정 날짜까지</option>
                      <option value="COUNT">횟수 지정</option>
                    </select>
                  </div>

                  {endType === 'UNTIL_DATE' && (
                    <div className="form-group">
                      <label className="form-label">종료 날짜</label>
                      <input className="form-input" type="date" value={endDate}
                             onChange={e => setEndDate(e.target.value)} />
                    </div>
                  )}

                  {endType === 'COUNT' && (
                    <div className="form-group">
                      <label className="form-label">반복 횟수</label>
                      <input className="form-input" type="number" min={1} max={365} value={endCount}
                             onChange={e => setEndCount(Number(e.target.value))} />
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>취소</button>
          <button className="btn btn-primary" onClick={handleSubmit} disabled={saving}>
            {saving ? '저장 중...' : (mode === 'create' ? '등록' : '저장')}
          </button>
        </div>
      </div>
    </div>
  );
}
