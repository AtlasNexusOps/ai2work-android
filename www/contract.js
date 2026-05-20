/**
 * AI2Work Contract Reader — Celo Mainnet
 * Reads ClaudelanceCore v2 bounties on-chain via JSON-RPC
 */
var CONTRACT = (function() {
  'use strict';

  var RPC = 'https://forno.celo.org';
  var CONTRACT_ADDR = '0x1362d874F40B7e28836cBeCcA14f5EfBe6c6E423';
  var TOKENS = {
    '0x765DE816845861e75A25fCA122bb6898B8B1282a': 'cUSD',
    '0x471EcE3750Da237f93B8E339c536989b8978a438': 'CELO',
    '0xcebA9300f2b948710d2653dD7B07f33A8B32118C': 'USDC'
  };

  // Minimal ABI — view functions only
  var ABI = {
    bountyCount:    { sig:'bountyCount()', returns:'uint256', inputs:[] },
    getBounty:      { sig:'getBounty(uint256)', returns:'tuple', inputs:[{type:'uint256'}] },
    getStats:       { sig:'getStats(address)', returns:'tuple', inputs:[{type:'address'}] },
    totalBountiesResolved: { sig:'totalBountiesResolved()', returns:'uint256', inputs:[] },
    uniqueWorkerCount: { sig:'uniqueWorkerCount()', returns:'uint256', inputs:[] },
  };

  // keccak256 of signature → first 4 bytes = selector
  function selector(sig) {
    // Simple hash — we use precomputed selectors
    var SEL = {
      'bountyCount()': '0x7e7186d8',
      'getBounty(uint256)': '0x72c9493e',
      'getStats(address)': '0xb37bdda2',
      'totalBountiesResolved()': '0x6e8f2d7d',
      'uniqueWorkerCount()': '0x6f0e1ff1',
    };
    return SEL[sig] || '0x00000000';
  }

  function encodeUint256(n) {
    return n.toString(16).padStart(64, '0');
  }

  function encodeAddress(addr) {
    return '000000000000000000000000' + addr.slice(2).toLowerCase();
  }

  function decodeUint256(hex) {
    return parseInt(hex.slice(0, 66), 16);
  }

  function decodeAddress(hex) {
    return '0x' + hex.slice(24, 64);
  }

  function decodeString(hex, offset) {
    var len = parseInt(hex.slice(offset*2, offset*2+64), 16);
    var start = offset*2 + 64;
    return hexToAscii(hex.slice(start, start + len*2));
  }

  function hexToAscii(hex) {
    var s = '';
    for (var i = 0; i < hex.length; i += 2) {
      var c = parseInt(hex.substr(i, 2), 16);
      if (c > 31 && c < 127) s += String.fromCharCode(c);
    }
    return s;
  }

  async function rpcCall(method, params) {
    var resp = await fetch(RPC, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        jsonrpc: '2.0', id: 1,
        method: method,
        params: params
      })
    });
    var json = await resp.json();
    if (json.error) throw new Error(json.error.message);
    return json.result;
  }

  async function ethCall(data) {
    return await rpcCall('eth_call', [{ to: CONTRACT_ADDR, data: data }, 'latest']);
  }

  // ── Public API ──

  async function getBountyCount() {
    var data = selector('bountyCount()');
    var result = await ethCall(data);
    return decodeUint256(result);
  }

  async function getBounty(id) {
    var data = selector('getBounty(uint256)') + encodeUint256(id);
    var result = await ethCall(data);
    // Struct Bounty layout (from ClaudelanceCore.sol):
    // id (uint256), poster (address), token (address), amount (uint256),
    // configHash (bytes32), deadline (uint256), maxSlots (uint256),
    // createdAt (uint256), status (uint8), bountyType (uint8),
    // targetWorker (address), // direct hire target
    // slotCount (uint256), resolvedWorkerCount (uint256)
    var off = 0;
    var _id = decodeUint256(result.slice(off, off+64)); off += 64;
    var poster = decodeAddress(result.slice(off, off+64)); off += 64;
    var token = decodeAddress(result.slice(off, off+64)); off += 64;
    var amount = decodeUint256(result.slice(off, off+64)); off += 64;
    // skip configHash
    off += 64;
    var deadline = decodeUint256(result.slice(off, off+64)); off += 64;
    var maxSlots = decodeUint256(result.slice(off, off+64)); off += 64;
    var createdAt = decodeUint256(result.slice(off, off+64)); off += 64;
    var status = decodeUint256(result.slice(off, off+64)); off += 64;
    var bountyType = decodeUint256(result.slice(off, off+64)); off += 64;
    var targetWorker = decodeAddress(result.slice(off, off+64)); off += 64;

    var tokenSymbol = TOKENS[token.toLowerCase()] || token.slice(0,6)+'…';
    var amountDisplay = (amount / 1e18).toFixed(0);
    var statusLabel = status === 0 ? 'Open' : status === 1 ? 'InProgress' : status === 2 ? 'Resolved' : status === 3 ? 'Cancelled' : 'Unknown';
    var typeLabel = bountyType === 1 ? 'Direct' : 'Open';

    return {
      id: Number(_id), poster: poster, token: tokenSymbol,
      amount: amountDisplay, deadline: Number(deadline),
      maxSlots: Number(maxSlots), status: statusLabel,
      bountyType: typeLabel, targetWorker: targetWorker,
      createdAt: Number(createdAt)
    };
  }

  async function getStats(tokenAddr) {
    var data = selector('getStats(address)') + encodeAddress(tokenAddr);
    var result = await ethCall(data);
    var off = 0;
    var totalVolume = decodeUint256(result.slice(off, off+64)); off += 64;
    var totalRevenue = decodeUint256(result.slice(off, off+64)); off += 64;
    var totalResolved = decodeUint256(result.slice(off, off+64)); off += 64;
    var uniquePosters = decodeUint256(result.slice(off, off+64)); off += 64;
    var uniqueWorkers = decodeUint256(result.slice(off, off+64)); off += 64;
    return {
      totalVolume: (totalVolume / 1e18).toFixed(0),
      totalRevenue: (totalRevenue / 1e18).toFixed(2),
      totalResolved: Number(totalResolved),
      uniquePosters: Number(uniquePosters),
      uniqueWorkers: Number(uniqueWorkers)
    };
  }

  async function getAllBounties() {
    var count = await getBountyCount();
    var bounties = [];
    var start = Math.max(1, count - 20); // last 20 bounties
    for (var i = start; i <= count; i++) {
      try {
        var b = await getBounty(i);
        if (b.status !== 'Unknown') bounties.push(b);
      } catch(e) { /* skip invalid bounties */ }
    }
    return bounties.reverse(); // newest first
  }

  async function getGlobalStats() {
    try {
      var cUsdStats = await getStats('0x765DE816845861e75A25fCA122bb6898B8B1282a');
      var resolved = Number(await rpcCall('eth_call', [{to:CONTRACT_ADDR, data:'0x6e8f2d7d'},'latest']).then(function(r){return parseInt(r,16)}));
      var workers = Number(await rpcCall('eth_call', [{to:CONTRACT_ADDR, data:'0x6f0e1ff1'},'latest']).then(function(r){return parseInt(r,16)}));
      return {
        totalVolume: cUsdStats.totalVolume,
        totalResolved: resolved,
        uniqueWorkers: workers
      };
    } catch(e) {
      return null;
    }
  }

  return {
    getAllBounties: getAllBounties,
    getBountyCount: getBountyCount,
    getGlobalStats: getGlobalStats
  };
})();
