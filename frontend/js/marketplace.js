import { api } from './api.js';

export function initMarketplace() {
    console.log("Initializing Marketplace Page...");
    const productsGrid = document.querySelector('.products-grid');
    const recentListingsContainer = document.getElementById('recent-listings-container');
    const categoryChips = document.querySelectorAll('.chip');
    const searchInput = document.getElementById('marketplace-search-input');
    const filterCategory = document.getElementById('filter-category');
    const filterCondition = document.getElementById('filter-condition');
    const filterLocation = document.getElementById('filter-location');
    const filterSort = document.getElementById('filter-sort');

    if (!productsGrid) return;

    // --- Sidebar Handling ---
    const rightPanel = document.querySelector('.right-panel');
    const marketplaceRightContent = document.getElementById('marketplace-right-sidebar-content');
    
    if (rightPanel && marketplaceRightContent) {
        // Store original content to restore later if needed
        if (!window.originalRightPanelContent) {
            window.originalRightPanelContent = rightPanel.innerHTML;
        }
        // Inject marketplace specific content
        rightPanel.innerHTML = marketplaceRightContent.innerHTML;
        
        // Bind popular search tags
        const tags = rightPanel.querySelectorAll('.search-tag-chip');
        tags.forEach(tag => {
            tag.addEventListener('click', () => {
                if (searchInput) {
                    searchInput.value = tag.textContent;
                    searchInput.dispatchEvent(new Event('input'));
                    loadProducts();
                }
            });
        });
    }

    // --- Product Loading Logic ---
    async function loadProducts() {
        try {
            const category = filterCategory ? filterCategory.value : '';
            const condition = filterCondition ? filterCondition.value : '';
            const location = filterLocation ? filterLocation.value : '';
            const sort = filterSort ? filterSort.value : '';
            const search = searchInput ? searchInput.value : '';

            // Note: api.getProducts might only support category. 
            // We'll pass category and then client-side filter for other fields to stay safe with existing API.
            let products = await api.getProducts(category === 'All' ? '' : category);

            // Client-side filtering for search, condition, location (Preserving backend compatibility)
            if (search) {
                products = products.filter(p => p.title.toLowerCase().includes(search.toLowerCase()));
            }
            if (condition) {
                products = products.filter(p => p.condition === condition);
            }
            if (location) {
                products = products.filter(p => p.location === location);
            }

            // Sorting
            if (sort === 'price_low') {
                products.sort((a, b) => (a.price || 0) - (b.price || 0));
            } else if (sort === 'price_high') {
                products.sort((a, b) => (b.price || 0) - (a.price || 0));
            }

            renderProducts(products);
            renderRecentProducts(products);
        } catch (error) {
            console.error("Load products error:", error);
            productsGrid.innerHTML = `<div class="error-msg">Failed to load products. Please try again later.</div>`;
        }
    }

    function renderProducts(products) {
        if (!products || products.length === 0) {
            productsGrid.innerHTML = `<div class="empty-msg" style="grid-column: 1/-1; text-align: center; padding: 60px; color: var(--text-muted);">
                <i class="fas fa-search" style="font-size: 40px; margin-bottom: 15px; opacity: 0.2;"></i>
                <p>No equipment found matching your criteria.</p>
            </div>`;
            return;
        }

        productsGrid.innerHTML = products.map(product => `
            <div class="listing-card-refined">
                <div class="card-img-wrap">
                    <img src="${product.imageUrl || product.images?.[0] || 'https://images.unsplash.com/photo-1594488311394-49a04a3777d1?q=80&w=400&auto=format&fit=crop'}" 
                         alt="${product.title}"
                         onerror="this.src='https://images.unsplash.com/photo-1594488311394-49a04a3777d1?q=80&w=400&auto=format&fit=crop'">
                    <button class="heart-btn"><i class="far fa-heart"></i></button>
                    ${product.isFeatured ? '<span class="featured-label">Featured</span>' : ''}
                </div>
                <div class="card-content">
                    <h4>${product.title}</h4>
                    <div class="card-metadata">
                        <span class="meta-item"><i class="fas fa-tag"></i> ${product.condition || 'Used'}</span>
                        <span class="meta-item"><i class="fas fa-cog"></i> ${product.category || 'Equipment'}</span>
                    </div>
                    <p class="card-location"><i class="fas fa-map-marker-alt" style="color: var(--primary-green);"></i> ${product.location || 'Maharashtra'}</p>
                    <div class="card-bottom">
                        <p class="card-price">₹${product.price?.toLocaleString() || 'Negotiable'}</p>
                        <button class="card-view-btn">View Details</button>
                    </div>
                </div>
            </div>
        `).join('');
    }

    function renderRecentProducts(products) {
        if (!recentListingsContainer) return;
        
        // Show last 5 products as "Recent"
        const recent = products.slice(-5).reverse();
        
        if (recent.length === 0) {
            recentListingsContainer.innerHTML = '<p style="padding: 20px; color: var(--text-muted); font-size: 13px;">No recent listings available.</p>';
            return;
        }

        recentListingsContainer.innerHTML = recent.map(product => `
            <div class="recent-item-compact">
                <img src="${product.imageUrl || product.images?.[0] || 'https://images.unsplash.com/photo-1594488311394-49a04a3777d1?q=80&w=400&auto=format&fit=crop'}" 
                     class="recent-thumb" alt="${product.title}"
                     onerror="this.src='https://images.unsplash.com/photo-1594488311394-49a04a3777d1?q=80&w=400&auto=format&fit=crop'">
                <div class="recent-details">
                    <h5>${product.title}</h5>
                    <p class="recent-meta-small">${product.location || 'Maharashtra'}</p>
                    <p class="recent-price-small">₹${product.price?.toLocaleString() || 'Negotiable'}</p>
                </div>
            </div>
        `).join('');
    }

    // --- Event Listeners ---
    
    // Category Chips
    categoryChips.forEach(chip => {
        chip.addEventListener('click', () => {
            categoryChips.forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            if (filterCategory) {
                filterCategory.value = chip.dataset.category || chip.textContent;
            }
            loadProducts();
        });
    });

    // Dropdown Filters
    [filterCategory, filterCondition, filterLocation, filterSort].forEach(el => {
        if (el) el.addEventListener('change', () => {
            // Update chips if category changed
            if (el === filterCategory) {
                categoryChips.forEach(c => {
                    c.classList.toggle('active', (c.dataset.category || c.textContent) === filterCategory.value);
                });
            }
            loadProducts();
        });
    });

    // Search Input
    if (searchInput) {
        let debounceTimer;
        searchInput.addEventListener('input', () => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => loadProducts(), 300);
        });
    }

    // Modal logic
    const addProductBtn = document.getElementById('add-product-btn');
    const addProductModal = document.getElementById('add-product-modal');
    const closeModalBtn = document.getElementById('close-modal-btn');
    const addProductForm = document.getElementById('add-product-form');

    if (addProductBtn && addProductModal) {
        addProductBtn.addEventListener('click', () => {
            addProductModal.style.display = 'flex';
        });
    }

    if (closeModalBtn && addProductModal) {
        closeModalBtn.addEventListener('click', () => {
            addProductModal.style.display = 'none';
        });
    }

    if (addProductForm) {
        addProductForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const newProduct = {
                title: document.getElementById('prod-name').value,
                category: document.getElementById('prod-category').value,
                condition: document.getElementById('prod-condition').value,
                price: parseFloat(document.getElementById('prod-price').value),
                location: document.getElementById('prod-location').value,
                description: document.getElementById('prod-desc').value,
                image: 'https://images.unsplash.com/photo-1594488311394-49a04a3777d1?q=80&w=400&auto=format&fit=crop',
                imageUrl: 'https://images.unsplash.com/photo-1594488311394-49a04a3777d1?q=80&w=400&auto=format&fit=crop',
                images: ['https://images.unsplash.com/photo-1594488311394-49a04a3777d1?q=80&w=400&auto=format&fit=crop']
            };

            try {
                await api.createListing(newProduct);
                alert('Product listed successfully!');
                addProductModal.style.display = 'none';
                addProductForm.reset();
                loadProducts();
            } catch (error) {
                alert('Failed to list product. Please try again.');
            }
        });
    }

    async function loadMarketRates() {
        const listEl = document.getElementById('market-rates-list');
        if (!listEl) return;
        try {
            const rates = await api.getMarketRates();
            if (!rates || !Array.isArray(rates) || rates.length === 0) {
                listEl.innerHTML = '<p style="font-size: 12px; color: var(--text-muted); text-align: center; padding: 10px;">No market rates available.</p>';
                return;
            }
            listEl.innerHTML = rates.slice(0, 5).map(r => `
                <div style="display: flex; justify-content: space-between; align-items: center; padding: 8px 10px; background: var(--bg-surface-soft, #f8fafc); border: 1px solid var(--border-color, #e5e7eb); border-radius: 8px;">
                    <div>
                        <strong style="font-size: 13px; color: var(--text-primary); display: block;">${r.commodity}</strong>
                        <span style="font-size: 11px; color: var(--text-muted);">${r.market}, ${r.state || ''}</span>
                    </div>
                    <div style="text-align: right;">
                        <span style="font-size: 13px; font-weight: 700; color: var(--primary-green, #16a34a);">₹${r.modalPrice}/q</span>
                        <span style="font-size: 10px; color: var(--text-muted); display: block;">₹${r.minPrice} - ₹${r.maxPrice}</span>
                    </div>
                </div>
            `).join('');
        } catch (err) {
            console.warn("Failed to load market rates:", err);
            listEl.innerHTML = '<p style="font-size: 12px; color: var(--text-muted); text-align: center; padding: 10px;">Market rates temporarily unavailable.</p>';
        }
    }

    async function loadMarketplaceStats() {
        try {
            const summary = await api.getMarketplaceSummary();
            const activeEl = document.getElementById('marketplace-active-count');
            const totalEl = document.getElementById('marketplace-total-count');
            if (summary) {
                const total = summary.totalProducts || summary.activeListings || 0;
                if (activeEl) activeEl.textContent = total;
                if (totalEl) totalEl.textContent = total;
            }
        } catch (err) {
            console.warn("Failed to load marketplace summary:", err);
        }
    }

    // Initial Load
    loadProducts();
    loadMarketRates();
    loadMarketplaceStats();
}

window.initMarketplacePage = initMarketplace;