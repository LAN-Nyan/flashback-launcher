async function loadGames() {
    try {
        const response = await fetch('/api/games');
        const library = await response.json();

        const gamesListContainer = document.getElementById('games-list'); // adjust selector
        gamesListContainer.innerHTML = ''; // Clear hardcoded HTML cards

        if (!library.games || library.games.length === 0) {
            gamesListContainer.innerHTML = '<p>No games found in games.json.</p>';
            return;
        }

        library.games.forEach(game => {
            const card = document.createElement('div');
            card.className = 'game-card';
            card.innerHTML = `
                <img src="${game.coverUrl || 'placeholder.png'}" alt="${game.title}">
                <h3>${game.title}</h3>
                <p>Platform: ${game.platform}</p>
            `;
            gamesListContainer.appendChild(card);
        });
    } catch (err) {
        console.error('Failed to load games:', err);
    }
}

// Load games on startup
document.addEventListener('DOMContentLoaded', loadGames);
