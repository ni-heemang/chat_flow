import api from './api';

export const authService = {
  async login(username, password) {
    try {
      const response = await api.post('/users/login', {
        username,
        password,
      });
      return response.data;
    } catch (error) {
      console.error('Login error:', error);
      console.error('Error response:', error.response);
      
      let errorMessage = '로그인 중 오류가 발생했습니다.';
      
      if (error.response) {
        // 서버에서 응답을 받은 경우
        const status = error.response.status;
        const data = error.response.data;
        
        console.log('Error status:', status);
        console.log('Error data:', data);
        
        if (status === 401) {
          errorMessage = '아이디 또는 비밀번호가 올바르지 않습니다.';
        } else if (data?.message) {
          errorMessage = data.message;
        } else if (data?.error) {
          errorMessage = data.error;
        }
      } else if (error.request) {
        // 요청은 보냈지만 응답을 받지 못한 경우
        errorMessage = '서버에 연결할 수 없습니다. 네트워크를 확인해주세요.';
      }
      
      throw new Error(errorMessage);
    }
  },

  async register(username, password, name) {
    try {
      const response = await api.post('/users/register', {
        username,
        password,
        name,
      });
      return response.data;
    } catch (error) {
      console.error('Register error:', error);
      console.error('Error response:', error.response);
      
      let errorMessage = '회원가입 중 오류가 발생했습니다.';
      
      if (error.response) {
        const status = error.response.status;
        const data = error.response.data;
        
        console.log('Error status:', status);
        console.log('Error data:', data);
        
        if (status === 400) {
          errorMessage = '입력 정보를 확인해주세요.';
        } else if (status === 409) {
          errorMessage = '이미 존재하는 아이디입니다.';
        } else if (data?.message) {
          errorMessage = data.message;
        } else if (data?.error) {
          errorMessage = data.error;
        }
      } else if (error.request) {
        errorMessage = '서버에 연결할 수 없습니다. 네트워크를 확인해주세요.';
      }
      
      throw new Error(errorMessage);
    }
  },

  async refreshToken(refreshToken) {
    try {
      const response = await api.post('/users/refresh', {
        refreshToken,
      });
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '토큰 갱신 중 오류가 발생했습니다.'
      );
    }
  },

  async getUserProfile() {
    try {
      const response = await api.get('/users/profile');
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '프로필 조회 중 오류가 발생했습니다.'
      );
    }
  },

  async updateProfile(profileData) {
    try {
      const response = await api.put('/users/profile', {
        name: profileData.name,
      });
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '프로필 업데이트 중 오류가 발생했습니다.'
      );
    }
  },
};

export default authService;