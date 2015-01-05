var MAIDSafe = {};
MAIDSafe.org = 'maidsafe'; // TODO set value from plugin
MAIDSafe.loadRepoOwners = function(repoSelect) {	
	var ownerSelect = jQuery(':input:eq(' + (jQuery(':input').index(repoSelect) + 1) + ')');
	var setOwners = function (data) {
		var options = '';
		for(var i = 0; i < data.length; i++) {		
			options += '<option id="' + data[i].owner.login + '">' + data[i].owner.login + '</option>';
		}
		ownerSelect.append(options);
	};
	ownerSelect.children().remove();
	MAIDSafe.getForks(repoSelect.val(), setOwners);
};
MAIDSafe.registerEvents = function(element) {
	jQuery(element).on('change', function(event) {
		MAIDSafe.loadRepoOwners(jQuery(event.target));
	});
	jQuery(':input:eq(' + (jQuery(':input').index(element) + 1) + ')').on('change', function(event) {
		var ownerSelect = jQuery(event.target);
		var repoSelect = jQuery(':input:eq(' + (jQuery(':input').index(event.target) - 1) + ')');
		var branchesSelect = jQuery(':input:eq(' + (jQuery(':input').index(event.target) + 1) + ')');
		var setBranches = function(data) {
			var options = '';
			for(var i = 0; i < data.length; i++) {		
				options += '<option id="' + data[i].owner.login + '">' + data[i].owner.login + '</option>';
			}
			ownerSelect.append(options);
		};
		branchesSelect.children().remove();
		MAIDSafe.getBranches(repoSelect.val(), ownerSelect.val(), setBranches);
	});
};
MAIDSafe.loadRepos = function(element) {	    			    		
	var options = '';
	if (!MAIDSafe.hasOwnProperty('repos')) {
		return;
	}		
	for(var i = 0; i < MAIDSafe.repos.length; i++) {		
		options += '<option id="' + MAIDSafe.repos[i].name + '">' + MAIDSafe.repos[i].name + '</option>';
	}
	if (!element) {
		element = jQuery('select[repo="true"]');
	}
	jQuery(element).append(options);
	MAIDSafe.registerEvents(element);
	MAIDSafe.loadRepoOwners(element);
};
MAIDSafe.getRepositories = function() {	    			
	jQuery.ajax({
		url:'https://api.github.com/orgs/' + MAIDSafe.org + '/repos',	    				
	 	crossDomain: true,
	 	headers: {
			'Authorization': 'token ' + MAIDSafe.token
		}
	})
	.success(function(data) {
		MAIDSafe.repos = data;
		MAIDSafe.loadRepos();			
	})
};
MAIDSafe.getForks = function(repoName, callback) {	
	jQuery.ajax({
		url:'https://api.github.com/repos/' + MAIDSafe.org + '/' + repoName + '/forks',	    				
	 	crossDomain: true,
	 	headers: {
			'Authorization': 'token ' + MAIDSafe.token
		}
	})
	.success(callback);	
};
MAIDSafe.getBranches = function(repo, owner, callback) {
	jQuery.ajax({
		url:'https://api.github.com/repos/' + owner + '/' + repo + '/forks',	    				
	 	crossDomain: true,
	 	headers: {
			'Authorization': 'token ' + MAIDSafe.token
		}
	})
	.success(callback);	
};
jQuery('document').ready(function() {	    			
	var element = jQuery('span#tempStore');	    			
	MAIDSafe.token = element.text();
	element.remove();
	MAIDSafe.getRepositories();
});
