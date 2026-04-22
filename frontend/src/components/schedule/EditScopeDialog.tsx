import { useState } from 'react';
import { EditScope } from '../../types';

interface Props {
  onSelect: (scope: EditScope) => void;
  onClose: () => void;
  action?: string;
}

const SCOPES: { value: EditScope; title: string; desc: string }[] = [
  { value: 'THIS', title: '이 일정만', desc: '선택한 날짜의 일정만 변경합니다.' },
  { value: 'THIS_AND_FOLLOWING', title: '이후 모든 일정', desc: '이 날짜 이후의 반복 일정을 모두 변경합니다.' },
  { value: 'ALL', title: '전체 일정', desc: '이 반복 시리즈의 모든 일정을 변경합니다.' },
];

export default function EditScopeDialog({ onSelect, onClose, action = '수정' }: Props) {
  const [selected, setSelected] = useState<EditScope>('THIS');

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 420 }}>
        <div className="modal-header">
          <h3>반복 일정 {action}</h3>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 16 }}>
            {action} 범위를 선택해주세요.
          </p>
          <div className="scope-options">
            {SCOPES.map(s => (
              <label key={s.value}
                     className={`scope-option ${selected === s.value ? 'selected' : ''}`}
                     onClick={() => setSelected(s.value)}>
                <input type="radio" name="scope" checked={selected === s.value}
                       onChange={() => setSelected(s.value)} />
                <div className="scope-option-text">
                  <h4>{s.title}</h4>
                  <p>{s.desc}</p>
                </div>
              </label>
            ))}
          </div>
        </div>
        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>취소</button>
          <button className="btn btn-primary" onClick={() => onSelect(selected)}>확인</button>
        </div>
      </div>
    </div>
  );
}
