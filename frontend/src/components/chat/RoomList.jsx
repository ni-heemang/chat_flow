import { useState, useEffect } from 'react';
import {
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  ListItemIcon,
  Paper,
  Typography,
  Box,
  Chip,
  CircularProgress,
  Alert,
  Fab,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Tabs,
  Tab,
  InputAdornment,
  IconButton,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Menu,
  ListItemSecondaryAction,
  Tooltip,
} from '@mui/material';
import {
  Chat as ChatIcon,
  Add as AddIcon,
  Group as GroupIcon,
  Search as SearchIcon,
  Clear as ClearIcon,
  FilterList as FilterIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Public as PublicIcon,
  Lock as PrivateIcon,
  LockOpen as PublicLockIcon,
  PersonAdd as JoinIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import useChatStore from '../../store/chatStore';
import useUserStore from '../../store/userStore';

const RoomList = () => {
  const navigate = useNavigate();
  const { user } = useUserStore();
  const {
    rooms,
    loading,
    error,
    fetchRooms,
    fetchMyRooms,
    createRoom,
    updateRoom,
    deleteRoom,
    clearError,
  } = useChatStore();

  // 탭 상태 (전체, 내 채팅방)
  const [tabValue, setTabValue] = useState(0);

  // 검색 및 필터 상태
  const [searchQuery, setSearchQuery] = useState('');
  const [filter, setFilter] = useState('all');

  // 채팅방 생성/수정 다이얼로그 상태
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editingRoom, setEditingRoom] = useState(null);
  const [roomForm, setRoomForm] = useState({
    name: '',
    description: '',
    maxParticipants: 50,
    isPublic: true,
  });

  // 메뉴 상태
  const [anchorEl, setAnchorEl] = useState(null);
  const [selectedRoom, setSelectedRoom] = useState(null);

  useEffect(() => {
    if (tabValue === 0) {
      fetchRooms(searchQuery, filter);
    } else {
      fetchMyRooms();
    }
  }, [tabValue, searchQuery, filter, fetchRooms, fetchMyRooms]);

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
    setSearchQuery('');
    setFilter('all');
  };

  const handleSearchChange = (event) => {
    setSearchQuery(event.target.value);
  };

  const handleSearchClear = () => {
    setSearchQuery('');
  };

  const handleFilterChange = (event) => {
    setFilter(event.target.value);
  };

  const handleRoomClick = (room) => {
    navigate(`/chat/${room.id}`, { state: { room } });
  };

  const handleMenuOpen = (event, room) => {
    event.stopPropagation();
    setAnchorEl(event.currentTarget);
    setSelectedRoom(room);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedRoom(null);
  };

  const handleEditRoom = () => {
    setEditingRoom(selectedRoom);
    setRoomForm({
      name: selectedRoom.name,
      description: selectedRoom.description || '',
      maxParticipants: selectedRoom.maxParticipants,
      isPublic: selectedRoom.isPublic,
    });
    setEditDialogOpen(true);
    handleMenuClose();
  };

  const handleDeleteRoom = async () => {
    if (window.confirm('정말로 이 채팅방을 삭제하시겠습니까?')) {
      const result = await deleteRoom(selectedRoom.id);
      if (result.success) {
        // 목록 새로고침
        if (tabValue === 0) {
          fetchRooms(searchQuery, filter);
        } else {
          fetchMyRooms();
        }
      }
    }
    handleMenuClose();
  };

  const handleCreateRoom = async () => {
    if (!roomForm.name.trim()) return;

    const result = await createRoom(roomForm);

    if (result.success) {
      setCreateDialogOpen(false);
      resetForm();
      // 내 채팅방 탭으로 이동
      setTabValue(1);
      // 내 채팅방 목록을 새로고침하여 생성된 채팅방이 즉시 표시되도록 함
      await fetchMyRooms();
    }
  };

  const handleUpdateRoom = async () => {
    if (!roomForm.name.trim() || !editingRoom) return;

    const result = await updateRoom(editingRoom.id, roomForm);

    if (result.success) {
      setEditDialogOpen(false);
      resetForm();
      setEditingRoom(null);
      // 목록 새로고침
      if (tabValue === 0) {
        fetchRooms(searchQuery, filter);
      } else {
        fetchMyRooms();
      }
    }
  };

  const resetForm = () => {
    setRoomForm({
      name: '',
      description: '',
      maxParticipants: 50,
      isPublic: true,
    });
  };

  const handleCloseDialog = () => {
    setCreateDialogOpen(false);
    setEditDialogOpen(false);
    setEditingRoom(null);
    resetForm();
    clearError();
  };

  if (loading && rooms.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h5" gutterBottom>
        채팅방 목록
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={clearError}>
          {error}
        </Alert>
      )}

      {/* 탭 */}
      <Paper elevation={2} sx={{ mb: 2 }}>
        <Tabs value={tabValue} onChange={handleTabChange} variant="fullWidth">
          <Tab label="전체 채팅방" />
          <Tab label="내 채팅방" />
        </Tabs>
      </Paper>

      {/* 검색 및 필터 (전체 채팅방 탭에서만) */}
      {tabValue === 0 && (
        <Paper elevation={1} sx={{ p: 2, mb: 2 }}>
          <Box display="flex" gap={2} alignItems="center">
            <TextField
              placeholder="채팅방 검색..."
              value={searchQuery}
              onChange={handleSearchChange}
              variant="outlined"
              size="small"
              sx={{ flexGrow: 1 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
                endAdornment: searchQuery && (
                  <InputAdornment position="end">
                    <IconButton size="small" onClick={handleSearchClear}>
                      <ClearIcon />
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>필터</InputLabel>
              <Select
                value={filter}
                label="필터"
                onChange={handleFilterChange}
              >
                <MenuItem value="all">전체</MenuItem>
                <MenuItem value="available">입장 가능</MenuItem>
                <MenuItem value="public">공개방</MenuItem>
                <MenuItem value="private">비공개방</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </Paper>
      )}

      <Paper elevation={2}>
        <List>
          {rooms.length === 0 ? (
            <ListItem>
              <ListItemText 
                primary={tabValue === 0 ? "검색된 채팅방이 없습니다" : "참여한 채팅방이 없습니다"}
                secondary={tabValue === 0 ? "다른 검색어로 시도해보세요!" : "새로운 채팅방을 만들어보세요!"}
              />
            </ListItem>
          ) : (
            rooms.map((room) => (
              <ListItem key={room.id} disablePadding>
                <ListItemButton onClick={() => handleRoomClick(room)}>
                  <ListItemIcon>
                    {room.isPublic ? (
                      <PublicLockIcon 
                        color="primary" 
                        sx={{ transform: 'scaleX(-1)' }}
                      />
                    ) : (
                      <PrivateIcon color="secondary" />
                    )}
                  </ListItemIcon>
                  <ListItemText
                    primary={
                      <Box display="flex" alignItems="center" gap={1}>
                        <Typography variant="h6">{room.name}</Typography>
                        <Chip
                          icon={<GroupIcon />}
                          label={`${room.currentParticipants || 0}/${room.maxParticipants}명`}
                          size="small"
                          color={room.currentParticipants >= room.maxParticipants ? "error" : "primary"}
                          variant="outlined"
                        />
                        {!room.isPublic && (
                          <Chip
                            label="비공개"
                            size="small"
                            color="secondary"
                            variant="outlined"
                          />
                        )}
                      </Box>
                    }
                    secondary={
                      <Box>
                        {room.description && (
                          <Typography variant="body2" color="text.secondary">
                            {room.description}
                          </Typography>
                        )}
                        <Typography variant="caption" color="text.secondary">
                          생성자: {room.createdByName} | 생성일: {new Date(room.createdAt).toLocaleDateString()}
                        </Typography>
                      </Box>
                    }
                  />
                  {/* 내 채팅방인 경우 관리 메뉴 */}
                  {tabValue === 1 && room.createdBy === user?.id && (
                    <ListItemSecondaryAction>
                      <IconButton
                        edge="end"
                        onClick={(e) => handleMenuOpen(e, room)}
                      >
                        <MoreVertIcon />
                      </IconButton>
                    </ListItemSecondaryAction>
                  )}
                </ListItemButton>
              </ListItem>
            ))
          )}
        </List>
      </Paper>

      {/* 메뉴 */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={handleEditRoom}>
          <EditIcon sx={{ mr: 1 }} />
          수정
        </MenuItem>
        <MenuItem onClick={handleDeleteRoom} sx={{ color: 'error.main' }}>
          <DeleteIcon sx={{ mr: 1 }} />
          삭제
        </MenuItem>
      </Menu>

      <Fab
        color="primary"
        aria-label="add"
        sx={{ position: 'fixed', bottom: 16, right: 16 }}
        onClick={() => setCreateDialogOpen(true)}
      >
        <AddIcon />
      </Fab>

      {/* 채팅방 생성 다이얼로그 */}
      <Dialog open={createDialogOpen} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>새 채팅방 만들기</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="채팅방 이름"
            fullWidth
            variant="outlined"
            value={roomForm.name}
            onChange={(e) => setRoomForm({ ...roomForm, name: e.target.value })}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="설명 (선택사항)"
            fullWidth
            variant="outlined"
            multiline
            rows={3}
            value={roomForm.description}
            onChange={(e) => setRoomForm({ ...roomForm, description: e.target.value })}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="최대 참여자 수"
            type="number"
            fullWidth
            variant="outlined"
            value={roomForm.maxParticipants}
            onChange={(e) => setRoomForm({ ...roomForm, maxParticipants: parseInt(e.target.value) || 50 })}
            inputProps={{ min: 2, max: 100 }}
            sx={{ mb: 2 }}
          />
          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>공개 설정</InputLabel>
            <Select
              value={roomForm.isPublic}
              label="공개 설정"
              onChange={(e) => setRoomForm({ ...roomForm, isPublic: e.target.value })}
            >
              <MenuItem value={true}>공개방</MenuItem>
              <MenuItem value={false}>비공개방</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog} disabled={loading}>
            취소
          </Button>
          <Button
            onClick={handleCreateRoom}
            variant="contained"
            disabled={!roomForm.name.trim() || loading}
          >
            {loading ? <CircularProgress size={20} /> : '만들기'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* 채팅방 수정 다이얼로그 */}
      <Dialog open={editDialogOpen} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>채팅방 수정</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="채팅방 이름"
            fullWidth
            variant="outlined"
            value={roomForm.name}
            onChange={(e) => setRoomForm({ ...roomForm, name: e.target.value })}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="설명 (선택사항)"
            fullWidth
            variant="outlined"
            multiline
            rows={3}
            value={roomForm.description}
            onChange={(e) => setRoomForm({ ...roomForm, description: e.target.value })}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="최대 참여자 수"
            type="number"
            fullWidth
            variant="outlined"
            value={roomForm.maxParticipants}
            onChange={(e) => setRoomForm({ ...roomForm, maxParticipants: parseInt(e.target.value) || 50 })}
            inputProps={{ min: 2, max: 100 }}
            sx={{ mb: 2 }}
          />
          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>공개 설정</InputLabel>
            <Select
              value={roomForm.isPublic}
              label="공개 설정"
              onChange={(e) => setRoomForm({ ...roomForm, isPublic: e.target.value })}
            >
              <MenuItem value={true}>공개방</MenuItem>
              <MenuItem value={false}>비공개방</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog} disabled={loading}>
            취소
          </Button>
          <Button
            onClick={handleUpdateRoom}
            variant="contained"
            disabled={!roomForm.name.trim() || loading}
          >
            {loading ? <CircularProgress size={20} /> : '수정'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default RoomList;