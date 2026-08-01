import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  site: 'https://xydesu.github.io',
  base: '/ChatConduit',
  integrations: [
    starlight({
      title: 'ChatConduit Wiki',
      description: 'Paper/Purpur 跨服聊天與社交外掛官方維基',
      social: {
        github: 'https://github.com/xydesu/ChatConduit',
      },
      sidebar: [
        {
          label: '指南與文檔',
          items: [
            { label: '🚀 概述與快速入門', link: '/' },
            { label: '💬 頻道與聊天系統', link: '/channels/' },
            { label: '🤝 好友與社交系統', link: '/friends/' },
            { label: '🔌 第三方整合與跨服', link: '/integrations/' },
          ],
        },
      ],
      defaultLocale: 'root',
      locales: {
        root: {
          label: '繁體中文',
          lang: 'zh-TW',
        },
      },
    }),
  ],
});
