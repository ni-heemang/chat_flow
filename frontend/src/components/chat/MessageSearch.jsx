import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Box,
  List,
  ListItem,
  ListItemText,
  ListItemAvatar,
  Avatar,
  Typography,
  Chip,
  Divider,
  InputAdornment,
  IconButton,
  Pagination,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
  FormControl,
  Select,
  MenuItem,
  InputLabel,
} from '@mui/material';
import {
  Search as SearchIcon,
  Close as CloseIcon,
  Person as PersonIcon,
  DateRange as DateIcon,
  Clear as ClearIcon,
} from '@mui/icons-material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { ko } from 'date-fns/locale';
import searchService from '../../services/searchService';
import messageService from '../../services/messageService';
import useChatStore from '../../store/chatStore';

const MessageSearch = ({ open, onClose, roomId, onMessageSelect }) => {
  const [tabValue, setTabValue] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  // 사용자별 검색
  const [selectedUser, setSelectedUser] = useState('');
  
  // 날짜별 검색
  const [startDate, setStartDate] = useState(null);
  const [endDate, setEndDate] = useState(null);

  const { participants } = useChatStore();
  const roomParticipants = participants[roomId] || [];

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
    setSearchResults([]);
    setCurrentPage(0);
    setError('');
  };

  const handleSearch = async (page = 0) => {
    if (!searchQuery.trim() && tabValue === 0) return;
    if (!selectedUser && tabValue === 1) return;
    if ((!startDate || !endDate) && tabValue === 2) return;

    setLoading(true);
    setError('');

    try {
      let result;
      
      switch (tabValue) {
        case 0: {
          // 키워드 검색 - 새로운 메시지 서비스 사용
          result = await messageService.searchMessages(roomId, searchQuery.trim(), page);
          break;
        }
        case 1: {
          // 사용자별 검색 - 기존 검색 서비스 사용
          result = await searchService.searchMessagesByUser(roomId, selectedUser, page);
          break;
        }
        case 2: {
          // 날짜별 검색 - 기존 검색 서비스 사용
          const startDateStr = startDate.toISOString().split('T')[0];
          const endDateStr = endDate.toISOString().split('T')[0];
          result = await searchService.searchMessagesByDate(roomId, startDateStr, endDateStr, page);
          break;
        }
        default:
          return;
      }

      if (result.success) {
        setSearchResults(result.data.content || []);
        setTotalPages(result.data.page?.totalPages || 0);
        setTotalElements(result.data.page?.totalElements || 0);
        setCurrentPage(page);
      } else {
        setError(result.error);
      }
    } catch (error) {
      console.error('Search error:', error);
      setError('검색 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handlePageChange = (event, page) => {
    handleSearch(page - 1); // MUI Pagination은 1부터 시작
  };

  const handleMessageClick = (message) => {
    if (onMessageSelect) {
      onMessageSelect(message);
    }
    onClose();
  };

  const handleClear = () => {
    setSearchQuery('');
    setSelectedUser('');
    setStartDate(null);
    setEndDate(null);
    setSearchResults([]);
    setCurrentPage(0);
    setError('');
  };

  const highlightText = (text, highlight) => {
    if (!highlight || tabValue !== 0) return text;
    
    const regex = new RegExp(`(${highlight})`, 'gi');
    const parts = text.split(regex);
    
    return parts.map((part, index) => 
      regex.test(part) ? (
        <mark key={index} style={{ backgroundColor: '#ffeb3b', padding: 0 }}>
          {part}
        </mark>
      ) : (
        part
      )
    );
  };

  const formatTimestamp = (timestamp) => {
    return new Date(timestamp).toLocaleString('ko-KR');
  };

  useEffect(() => {
    if (!open) {
      handleClear();
    }
  }, [open]);

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      PaperProps={{
        sx: { height: '80vh' }
      }}
    >
      <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h6">메시지 검색</Typography>
        <IconButton onClick={onClose}>
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      
      <DialogContent dividers>
        <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
          <Tabs value={tabValue} onChange={handleTabChange}>
            <Tab icon={<SearchIcon />} label="키워드 검색" />
            <Tab icon={<PersonIcon />} label="사용자별" />
            <Tab icon={<DateIcon />} label="날짜별" />
          </Tabs>
        </Box>

        {/* 검색 입력 영역 */}
        <Box sx={{ mb: 2 }}>
          {tabValue === 0 && (
            <TextField
              fullWidth
              placeholder="검색할 키워드를 입력하세요"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyPress={(e) => {
                if (e.key === 'Enter') {
                  handleSearch();
                }
              }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
                endAdornment: searchQuery && (
                  <InputAdornment position="end">
                    <IconButton onClick={() => setSearchQuery('')}>
                      <ClearIcon />
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
          )}

          {tabValue === 1 && (
            <FormControl fullWidth>
              <InputLabel>사용자 선택</InputLabel>
              <Select
                value={selectedUser}
                label="사용자 선택"
                onChange={(e) => setSelectedUser(e.target.value)}
              >
                {roomParticipants.map((participant) => (
                  <MenuItem key={participant.username} value={participant.username}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Avatar sx={{ width: 24, height: 24 }}>
                        {(participant.name || participant.username)[0].toUpperCase()}
                      </Avatar>
                      {participant.name || participant.username}
                    </Box>
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}

          {tabValue === 2 && (
            <LocalizationProvider dateAdapter={AdapterDateFns} adapterLocale={ko}>
              <Box sx={{ display: 'flex', gap: 2 }}>
                <DatePicker
                  label="시작 날짜"
                  value={startDate}
                  onChange={setStartDate}
                  renderInput={(params) => <TextField {...params} />}
                />
                <DatePicker
                  label="종료 날짜"
                  value={endDate}
                  onChange={setEndDate}
                  renderInput={(params) => <TextField {...params} />}
                />
              </Box>
            </LocalizationProvider>
          )}
        </Box>

        {/* 검색 버튼 */}
        <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
          <Button
            variant="contained"
            startIcon={<SearchIcon />}
            onClick={() => handleSearch()}
            disabled={loading || 
              (tabValue === 0 && !searchQuery.trim()) ||
              (tabValue === 1 && !selectedUser) ||
              (tabValue === 2 && (!startDate || !endDate))
            }
          >
            검색
          </Button>
          <Button
            variant="outlined"
            startIcon={<ClearIcon />}
            onClick={handleClear}
          >
            초기화
          </Button>
        </Box>

        {/* 검색 결과 */}
        <Box sx={{ flexGrow: 1 }}>
          {loading && (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          )}

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          {!loading && searchResults.length === 0 && (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <Typography color="text.secondary">
                검색 결과가 없습니다
              </Typography>
            </Box>
          )}

          {!loading && searchResults.length > 0 && (
            <>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                총 {totalElements}개의 메시지를 찾았습니다
              </Typography>
              
              <List>
                {searchResults.map((message, index) => (
                  <div key={message.id}>
                    <ListItem
                      button
                      onClick={() => handleMessageClick(message)}
                      alignItems="flex-start"
                    >
                      <ListItemAvatar>
                        <Avatar>
                          {(message.name || message.username)[0].toUpperCase()}
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="subtitle2">
                              {message.name || message.username}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {formatTimestamp(message.timestamp)}
                            </Typography>
                            {message.messageType === 'SYSTEM' && (
                              <Chip size="small" label="시스템" color="info" />
                            )}
                          </Box>
                        }
                        secondary={
                          <Typography variant="body2" sx={{ mt: 0.5 }}>
                            {highlightText(message.content, searchQuery)}
                          </Typography>
                        }
                      />
                    </ListItem>
                    {index < searchResults.length - 1 && <Divider />}
                  </div>
                ))}
              </List>

              {totalPages > 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
                  <Pagination
                    count={totalPages}
                    page={currentPage + 1}
                    onChange={handlePageChange}
                    color="primary"
                  />
                </Box>
              )}
            </>
          )}
        </Box>
      </DialogContent>
      
      <DialogActions>
        <Button onClick={onClose}>닫기</Button>
      </DialogActions>
    </Dialog>
  );
};

export default MessageSearch;